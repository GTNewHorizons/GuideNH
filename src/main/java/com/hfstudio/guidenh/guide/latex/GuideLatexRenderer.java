package com.hfstudio.guidenh.guide.latex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideLatexRenderer {

    public static final GuideLatexRenderer INSTANCE = new GuideLatexRenderer();

    private static final int DEFAULT_FILL_COLOR_ARGB = ColorUtils.WHITE.getColor();

    /** Calibration formula used to determine a reference character height at a given sourceScale. */
    private static final String CALIBRATION_FORMULA = "x";
    private static final int MAX_REF_HEIGHT_ENTRIES = 16;

    /** Maps sourceScale float (rounded to string) -> calibrated reference height in pixels. */
    private final ConcurrentHashMap<String, Integer> refHeightCache = new ConcurrentHashMap<>();

    /** Lazily queried {@code GL_MAX_TEXTURE_SIZE}; only touched on the render thread. */
    private int maxTextureSize;

    /** Render-thread scratch state for {@link #readModelViewScale()}. */
    private final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    private float modelViewScaleX = 1f;
    private float modelViewScaleY = 1f;

    protected GuideLatexRenderer() {}

    /**
     * Returns the calibrated reference height (in pixels) for {@code "x"} rendered at
     * {@code sourceScale}. Subsequent calls with the same scale are instant (cached).
     * Safe to call from any thread; does not touch OpenGL.
     *
     * @param sourceScale jlatexmath render size parameter
     * @return pixel height of a lower-case "x" glyph at the given scale
     */
    public int calibrateRefHeight(float sourceScale) {
        String key = GuideLatexTextureCache.buildScaleKey(sourceScale);
        Integer height = refHeightCache.computeIfAbsent(key, k -> {
            try {
                TeXIcon icon = createIcon(CALIBRATION_FORMULA, DEFAULT_FILL_COLOR_ARGB, sourceScale);
                int h = icon.getIconHeight();
                return Math.max(1, h);
            } catch (ParseException e) {
                GuideDebugLog.warn("[GuideNH/LaTeX] Failed to calibrate reference height for scale {}", sourceScale, e);
                return 16;
            }
        });
        trimRefHeightCacheIfNeeded();
        return height;
    }

    private void trimRefHeightCacheIfNeeded() {
        if (refHeightCache.size() <= MAX_REF_HEIGHT_ENTRIES) {
            return;
        }
        for (Map.Entry<String, Integer> entry : refHeightCache.entrySet()) {
            if (refHeightCache.size() <= MAX_REF_HEIGHT_ENTRIES) {
                return;
            }
            refHeightCache.remove(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the pixel dimensions {@code [widthPx, heightPx, depthPx]} of {@code formula} rendered at
     * {@code sourceScale}, or {@code null} if the formula is invalid/failed.
     *
     * <p>
     * {@code depthPx} is the typographic depth in jlatexmath pixels, the number of pixels the formula
     * extends <em>below</em> its math baseline (e.g. denominators in fractions). For formulas with no
     * descenders (letters, superscripts) this is {@code 0}.
     *
     * <p>
     * Safe to call from any thread; does NOT upload any OpenGL texture.
     *
     * @param formula       LaTeX source string
     * @param fillColorArgb ARGB colour (only used for cache key uniformity; does not affect size)
     * @param sourceScale   jlatexmath render size parameter
     * @return [widthPx, heightPx, depthPx] or null on parse failure
     */
    public int[] measureSize(String formula, int fillColorArgb, float sourceScale) {
        if (formula == null || formula.isEmpty()) {
            return null;
        }
        if (GuideLatexTextureCache.INSTANCE.hasFailed(formula)) {
            return null;
        }

        String sizeKey = GuideLatexTextureCache.buildSizeCacheKey(formula, sourceScale);
        int[] cached = GuideLatexTextureCache.INSTANCE.getSize(sizeKey);
        if (cached != null) {
            return cached;
        }

        try {
            TeXIcon icon = createIcon(formula, fillColorArgb, sourceScale);
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            int d = getIconDepthPx(icon);
            GuideLatexTextureCache.INSTANCE.putSize(sizeKey, w, h, d);
            return new int[] { w, h, d };
        } catch (ParseException e) {
            GuideDebugLog.warn("[GuideNH/LaTeX] Parse error measuring '{}': {}", formula, e.getMessage());
            GuideLatexTextureCache.INSTANCE.markFailed(formula, e.getMessage());
            return null;
        } catch (Exception e) {
            GuideDebugLog.warn("[GuideNH/LaTeX] Unexpected error measuring '{}': {}", formula, e.getMessage(), e);
            GuideLatexTextureCache.INSTANCE.markFailed(formula, e.getMessage());
            return null;
        }
    }

    /**
     * Rasterizes {@code formula} at the physical pixel size it occupies on screen and draws it into the GUI-space
     * rectangle {@code (x, y, displayW, displayH)}.
     *
     * <p>
     * The texture is rendered at {@code displayW/H * guiScaleFactor * modelViewScale} pixels instead of the (much
     * larger) {@code sourceScale} size, so the GPU barely has to minify it. Minifying a large texture with plain
     * bilinear sampling skips whole texel rows, which made thin strokes such as fraction bars, minus signs and
     * small superscripts disappear on 1080p-class displays.
     * Must be called from the Minecraft render thread.
     *
     * @return {@code true} if the formula was drawn, {@code false} if it failed to parse or render
     */
    public boolean drawLatex(String formula, int fillColorArgb, float sourceScale, int x, int y, int displayW,
        int displayH) {
        if (displayW <= 0 || displayH <= 0) {
            return false;
        }
        int guiScale = Math.max(1, DisplayScale.scaleFactor());
        readModelViewScale();
        long texW = (long) Math.ceil(displayW * GuideLatexRasterMath.quantizePixelScale(guiScale * modelViewScaleX));
        long texH = (long) Math.ceil(displayH * GuideLatexRasterMath.quantizePixelScale(guiScale * modelViewScaleY));
        int maxSize = getMaxTextureSize();
        if (texW > maxSize || texH > maxSize) {
            double shrink = Math.min((double) maxSize / texW, (double) maxSize / texH);
            texW = Math.max(1, (long) Math.floor(texW * shrink));
            texH = Math.max(1, (long) Math.floor(texH * shrink));
        }
        int[] tex = getOrCreateTexture(formula, fillColorArgb, sourceScale, (int) texW, (int) texH);
        if (tex == null) {
            return false;
        }
        renderLatex(x, y, displayW, displayH, tex[0]);
        return true;
    }

    /**
     * Returns (and caches) the OpenGL texture for {@code formula} at its native {@code sourceScale} size.
     * Prefer {@link #drawLatex}, which rasterizes at the on-screen resolution.
     * Must be called from the Minecraft render thread.
     *
     * @return [textureId, widthPx, heightPx] or null on failure
     */
    public int[] getOrCreateTexture(String formula, int fillColorArgb, float sourceScale) {
        return getOrCreateTexture(formula, fillColorArgb, sourceScale, -1, -1);
    }

    /**
     * Returns (and caches) the OpenGL texture for {@code formula}, rasterized into a {@code targetW x targetH}
     * pixel image. The formula is laid out at {@code sourceScale} and then scaled as vector geometry, so the
     * result is properly anti-aliased at the target resolution. Non-positive target sizes use the native icon size.
     * Must be called from the Minecraft render thread.
     *
     * @return [textureId, widthPx, heightPx] or null on failure
     */
    public int[] getOrCreateTexture(String formula, int fillColorArgb, float sourceScale, int targetW, int targetH) {
        if (formula == null || formula.isEmpty()) {
            return null;
        }
        if (GuideLatexTextureCache.INSTANCE.hasFailed(formula)) {
            return null;
        }

        String texKey = GuideLatexTextureCache
            .buildTextureCacheKey(formula, fillColorArgb, sourceScale, Math.max(0, targetW), Math.max(0, targetH));
        int[] cached = GuideLatexTextureCache.INSTANCE.getTexture(texKey);
        if (cached != null) {
            return cached;
        }

        try {
            TeXIcon icon = createIcon(formula, fillColorArgb, sourceScale);

            String sizeKey = GuideLatexTextureCache.buildSizeCacheKey(formula, sourceScale);
            GuideLatexTextureCache.INSTANCE
                .putSize(sizeKey, icon.getIconWidth(), icon.getIconHeight(), getIconDepthPx(icon));

            BufferedImage image = renderToImage(icon, targetW, targetH);
            int w = image.getWidth();
            int h = image.getHeight();

            int textureId = uploadToGL(image, w, h);
            GuideLatexTextureCache.INSTANCE.putTexture(texKey, textureId, w, h);
            return new int[] { textureId, w, h };
        } catch (ParseException e) {
            GuideDebugLog.warn("[GuideNH/LaTeX] Parse error rendering '{}': {}", formula, e.getMessage());
            GuideLatexTextureCache.INSTANCE.markFailed(formula, e.getMessage());
            return null;
        } catch (Exception e) {
            GuideDebugLog.warn("[GuideNH/LaTeX] Unexpected error rendering '{}': {}", formula, e.getMessage(), e);
            GuideLatexTextureCache.INSTANCE.markFailed(
                formula,
                e.getMessage() == null ? e.getClass()
                    .getSimpleName() : e.getMessage());
            return null;
        }
    }

    /**
     * Renders a previously created texture as a quad at the specified screen position.
     * Must be called from the Minecraft render thread.
     *
     * @param x         screen X (document-relative)
     * @param y         screen Y (document-relative)
     * @param displayW  rendered display width in GUI units
     * @param displayH  rendered display height in GUI units
     * @param textureId OpenGL texture ID obtained from {@link #getOrCreateTexture}
     */
    public void renderLatex(int x, int y, int displayW, int displayH, int textureId) {
        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT | GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            // The GUI render context enables an alpha test (alpha > 0.1). It would discard the faint
            // anti-aliased pixels that make up thin strokes such as fraction bars and minus signs.
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ColorUtils.applyGlColor(ColorUtils.WHITE.getColor());

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x, y + displayH, 0, 0.0, 1.0);
            tess.addVertexWithUV(x + displayW, y + displayH, 0, 1.0, 1.0);
            tess.addVertexWithUV(x + displayW, y, 0, 1.0, 0.0);
            tess.addVertexWithUV(x, y, 0, 0.0, 0.0);
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static TeXIcon createIcon(String formula, int fillColorArgb, float sourceScale) throws ParseException {
        TeXFormula texFormula = new TeXFormula(formula);
        TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
            .setSize(sourceScale)
            .setFGColor(new Color(fillColorArgb, true))
            .build();
        icon.setInsets(new Insets(2, 2, 2, 2));
        icon.setForeground(new Color(fillColorArgb, true));
        return icon;
    }

    /**
     * Reads the X/Y scale of the current modelview matrix (document zoom, Mermaid node scaling, ...) so the texture
     * matches the physical pixel size the quad will cover.
     */
    private void readModelViewScale() {
        modelViewBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        float m00 = modelViewBuffer.get(0);
        float m01 = modelViewBuffer.get(1);
        float m10 = modelViewBuffer.get(4);
        float m11 = modelViewBuffer.get(5);
        float sx = (float) Math.sqrt(m00 * m00 + m01 * m01);
        float sy = (float) Math.sqrt(m10 * m10 + m11 * m11);
        modelViewScaleX = Float.isFinite(sx) && sx > 0.01f ? sx : 1f;
        modelViewScaleY = Float.isFinite(sy) && sy > 0.01f ? sy : 1f;
    }

    private int getMaxTextureSize() {
        int size = maxTextureSize;
        if (size <= 0) {
            size = Math.max(1024, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
            maxTextureSize = size;
        }
        return size;
    }

    private static int getIconDepthPx(TeXIcon icon) {
        return Math.max(0, (int) Math.ceil(icon.getTrueIconDepth()));
    }

    private static BufferedImage renderToImage(TeXIcon icon, int targetW, int targetH) {
        int iconW = Math.max(1, icon.getIconWidth());
        int iconH = Math.max(1, icon.getIconHeight());
        int w = targetW > 0 ? targetW : iconW;
        int h = targetH > 0 ? targetH : iconH;

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // LCD (sub-pixel) text AA is meaningless on a translucent texture; use grayscale coverage AA.
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g.setColor(new Color(ColorUtils.TRANSPARENT.getColor(), true));
            g.fillRect(0, 0, w, h);

            if (w != iconW || h != iconH) {
                // Scale the vector output instead of the bitmap so strokes are rasterized with correct coverage.
                g.scale((double) w / iconW, (double) h / iconH);
            }
            icon.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int uploadToGL(BufferedImage image, int w, int h) {
        int[] pixels = ((DataBufferInt) image.getRaster()
            .getDataBuffer()).getData();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // Upload a full area-averaged mip chain so the formula stays legible (no skipped strokes) even when a
        // parent transform draws it smaller than the resolution it was rasterized at.
        int level = 0;
        int levelW = w;
        int levelH = h;
        int[] levelPixels = pixels;
        while (true) {
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                level,
                GL11.GL_RGBA8,
                levelW,
                levelH,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                toRgbaBuffer(levelPixels));
            if (levelW == 1 && levelH == 1) {
                break;
            }
            int nextW = Math.max(1, levelW >> 1);
            int nextH = Math.max(1, levelH >> 1);
            levelPixels = GuideLatexRasterMath.downsample(levelPixels, levelW, levelH, nextW, nextH);
            levelW = nextW;
            levelH = nextH;
            level++;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, level);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }

    private static ByteBuffer toRgbaBuffer(int[] pixels) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }
}
