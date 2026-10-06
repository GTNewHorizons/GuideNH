package com.hfstudio.guidenh.guide.latex;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.nio.ByteBuffer;
import java.text.AttributedCharacterIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Typeset-at-target-size LaTeX renderer.
 *
 * <p>
 * Formulas are typeset directly at their final pixel font size
 * ({@code fontSize} = body base font size × user scale) via
 * {@code TeXIconBuilder.setSize(fontSize)} - no 100px oversized texture, no
 * scaling, no reference-string calibration. The returned icon pixel
 * dimensions ARE the display dimensions. The only retained mechanism is the
 * {@code TeXIcon#getBaseLine()} ratio cache, which feeds the inline baseline
 * anchoring chain (see {@code LytLatexBlock}).
 *
 * <p>
 * <b>Device-resolution rasterization.</b> The OpenGL texture is rasterized
 * at {@code fontSize × rasterScale} device pixels (rasterScale = the real
 * output scale: in-game {@code DisplayScale.scaleFactor()}, headless the
 * offscreen {@code PushTransform(0,0,scale)} wrap), while the blit quad keeps
 * the document {@code formulaDisplayW/H} dimensions. Because the blit quad is
 * scaled by the same factor to device pixels, texture pixels map 1:1 onto
 * device pixels (no GL_LINEAR upscale blur) - the same contract as the text
 * path, which rasterizes glyphs at {@code font_size × render_scale}
 * (parley_text.rs:834).
 *
 * <p>
 * The texture is created lazily at blit time: blocks emit a stable
 * negative <em>blit token</em> (never a GL texture id) via
 * {@link #registerLatexBlit}; {@code GuideRenderEngine.drawBlitTexture}
 * resolves the real texture via {@link #resolveLatexTexture} with the exact
 * device scale of the current transform stack, which is the only point where
 * the headless output scale is known (the offscreen {@code PushTransform}
 * wrap is applied after primitive collection).
 */
public class GuideLatexRenderer {

    public static final GuideLatexRenderer INSTANCE = new GuideLatexRenderer();

    private static final int DEFAULT_FILL_COLOR_ARGB = ColorUtils.WHITE.getColor();

    /**
     * Supersampling factor applied to formula texture rasterization.
     *
     * <p>
     * <b>Mechanism.</b> jlatexmath's built-in CM math fonts (jlm_cmmi10.ttf
     * etc.) carry no TrueType hinting tables (no cvt/fpgm/prep), so glyph stems
     * (physical width 2.5-3.1 device px at the output scales in use) cannot be
     * snapped to the pixel grid when rasterized 1:1 at device resolution: core
     * pixels stay under-weighted and the AA fringe dominates, making strokes
     * read visually thinner than body text (msyh, which IS hinted).
     * Supersampling attacks this in the pure pixel domain: the formula is
     * typeset and rasterized internally at {@code fontSize × rasterScale × S}
     * and then bilinearly downsampled back to {@code fontSize × rasterScale}.
     * The multi-sample energy of a sub-pixel stem is redistributed onto the
     * target grid, restoring core-pixel weight without changing any geometry.
     *
     * <p>
     * <b>Relationship to {@link #LATEX_EMBOLDEN_EM} (probe-decided:
     * stack, not replace).</b> An offline probe harness (real
     * jlatexmath-1.0.7.jar + bundled CM fonts, full-bright = alpha>200 over
     * alpha>30 ink) measured the in-game default scale (s=2, fontSize 11):
     * outline embolden ALONE at S=1 only reaches 0.31-0.47 full-bright
     * (unreachable for the ≥0.65 target), while S=3 alone is marginal
     * (0.603 for 'x'). S=3 + em=0.010 reaches ≥0.70 at s=2 and ≥0.82 at s=4
     * across the corpus - hence the two mechanisms are STACKED: supersampling
     * restores sub-pixel stem energy, the outline stroke adds physical stem
     * width. See {@link #LATEX_EMBOLDEN_EM} for the outline mechanism.
     *
     * <p>
     * <b>Resolution contract boundary.</b> This constant ONLY raises the internal
     * rasterization resolution. The final texture dimensions stay exactly the
     * device-resolution {@code fontSize × rasterScale} icon size (see
     * {@link #getOrCreateTexture}: the target size icon is built at
     * {@code fontSize × rasterScale}), and the blit keeps its 1:1 device-pixel
     * mapping with GL_LINEAR - nothing in the layout/typesetting chain or the
     * texture upload/quad path changes. Geometry never enters Java: this is a
     * pure rasterization (pixel) layer change.
     */
    private static final float LATEX_SUPERSAMPLE_S = 3f;

    /**
     * Outline-domain emboldening stroke width, in em units of the internal
     * rasterization em ({@code em = fontSize × rasterScale × S}).
     *
     * <p>
     * <b>Mechanism (MathJax SVG "blacker" - Java2D equivalent).</b>
     * MathJax SVG output strokes every glyph path with the same colour as its
     * fill: {@code path[data-c], use[data-c] { stroke-width: blacker }} with
     * {@code stroke: currentColor} inherited from the root group
     * (MathJax-src {@code ts/output/svg/Wrappers/TextNode.ts} addStyles;
     * {@code ts/output/svg/SVG.ts} OPTIONS {@code blacker: 3}); i.e. each
     * glyph outline is drawn fill(currentColor) + stroke(currentColor,
     * stroke-width=3/1000em). KaTeX does the same at font-build time
     * (METAFONT blacker 15 + ttfautohint). Outline-first-then-rasterize is the
     * standard answer for unhinted thin stems; pixel-domain compensation
     * (supersampling) is not a substitute (see {@link #LATEX_SUPERSAMPLE_S}).
     * In Java2D this is done by intercepting {@code Graphics2D.drawChars} (the
     * only text path jlatexmath uses - {@code CharBox.draw} →
     * {@code g2.drawChars}) in {@link #renderToImage} and re-rendering each
     * glyph as its {@code GlyphVector} outline: fill + same-colour
     * {@link java.awt.BasicStroke} (see {@link EmboldenGraphics2D}).
     *
     * <p>
     * <b>Em conversion (constant device-pixel stroke, NOT per-glyph
     * em-ratio).</b> The internal rasterization em is
     * {@code fontSize × rasterScale × S} device px of the supersampled image
     * (typeset via {@code setSize(fontSize × rasterScale × S)}), so the stroke
     * width in that image is {@code LATEX_EMBOLDEN_EM × fontSize × rasterScale
     * × S} px; after the S× bilinear downsample the device stroke is
     * {@code LATEX_EMBOLDEN_EM × fontSize × rasterScale} px. That is a CONSTANT
     * number of device pixels for every glyph - script/script-script glyphs do
     * NOT get an em-proportional thinner stroke, because the script size factor
     * is already inside CharBox's transform scale and
     * {@code strokeUserWidth()} divides it out again (see
     * {@code EmboldenGraphics2D}). This is the deliberate difference from
     * MathJax's em-relative {@code blacker} (3/1000em per glyph at that glyph's
     * own em), kept for three reasons: script glyphs have the thinnest stems
     * and need the full stroke for readability; METAFONT blacker is likewise
     * not an em-ratio adjustment per size; and the whole probe calibration
     * (next paragraph) was measured on this constant-device-pixel behaviour.
     *
     * <p>
     * <b>Value (probe-calibrated from the MathJax default).</b> MathJax's
     * default blacker is 3/1000em; at our em scales (44px at s=4, 22px at
     * s=2) that is a 0.13/0.07 px stroke - sub-pixel, insufficient (probe:
     * S=3+0.003 → 'x' 0.616 full-bright at s=2, below the 0.65 target).
     * The same probe harness (real jlatexmath jar + CM fonts) swept
     * 0.003-0.030em; 0.010 (=10/1000em, ~0.44/0.22 px device stroke) is the
     * smallest value reaching ≥0.70 full-bright at s=2 and ≥0.82 at s=4 for
     * the corpus, hence the default. All these numbers (including the
     * counter-closure measurements below) were measured under the
     * constant-device-pixel stroke described above - script glyphs included -
     * so switching to MathJax's per-glyph em-ratio mechanism would invalidate
     * them and require re-running the entire calibration, with script stems
     * ending up thinner than the values below were chosen to keep readable.
     *
     * <p>
     * <b>Counter-closure risk (probe-measured at s=4).</b> blacker too
     * large closes glyph counters (MathJax community reference: >20/1000em
     * starts closing details). Probe measurement of interior white
     * (alpha≤30 enclosed) at s=4 S=3 fontSize 11: at 0.010 the counters
     * shrink by ≤15% (e: 464→394, a: 1176→1093, g: 1142→1035 px) and NO hole
     * is destroyed for e/a/g/√x; {@code \int} and {@code \sum} have no
     * enclosed counter at this resolution; the {@code \frac{a}{b}} interior
     * (2327→2131 px, -8%) and its rule stay open. Closure appears only far
     * above the default (0.030 shrinks e's counter to 304 px but still open),
     * so 0.010 sits well inside the safe range.
     *
     * <p>
     * <b>Contract.</b> This is a pure rasterization-layer change inside
     * {@link #renderToImage}: the final texture dimensions, the 1:1 blit
     * geometry and the GL_LINEAR upload are untouched. 0 disables emboldening
     * (plain {@code drawChars} path) for A/B comparison.
     */
    private static final float LATEX_EMBOLDEN_EM = 0.010f;

    /**
     * Maps (fontSize:style:formula) size-key -> the TeXIcon baseline ratio
     * from {@link TeXIcon#getBaseLine()}: the distance from the icon's top edge
     * to its math baseline, expressed as a fraction of the icon's total height
     * (insets included), in {@code [0,1]}. Kept in exact float form so the
     * inline anchor computation never round-trips through an intermediate
     * ceil of the icon depth (see {@code LytLatexBlock}).
     */
    private final ConcurrentHashMap<String, Float> baselineRatioCache = new ConcurrentHashMap<>();

    protected GuideLatexRenderer() {}

    /**
     * Returns the pixel dimensions {@code [widthPx, heightPx, depthPx]} of {@code formula} typeset at
     * {@code fontSize} with the given jlatexmath {@code style}, or {@code null} if the formula is
     * invalid/failed.
     *
     * <p>
     * {@code widthPx}/{@code heightPx} are the full TeXIcon pixel dimensions (the true 2px/side
     * icon insets included) and ARE the display dimensions - no scaling happens downstream. This is
     * the typeset-at-target-size contract: {@code setSize(fontSize)} typesets the formula at exactly
     * {@code fontSize} pixels, so the icon width/height match the blit box 1:1.
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
     * @param fontSize      target typeset font size in pixels (body base font size × user scale)
     * @param style         jlatexmath style constant ({@link TeXConstants#STYLE_DISPLAY} or
     *                      {@link TeXConstants#STYLE_TEXT})
     * @return [widthPx, heightPx, depthPx] or null on parse failure
     */
    public int[] measureSize(String formula, int fillColorArgb, float fontSize, int style) {
        if (formula == null || formula.isEmpty()) {
            return null;
        }
        if (GuideLatexTextureCache.INSTANCE.hasFailed(formula)) {
            return null;
        }

        String sizeKey = GuideLatexTextureCache.buildSizeCacheKey(formula, fontSize, style);
        int[] cached = GuideLatexTextureCache.INSTANCE.getSize(sizeKey);
        if (cached != null) {
            return cached;
        }

        try {
            TeXFormula texFormula = new TeXFormula(formula);
            TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(style)
                .setSize(fontSize)
                .setFGColor(new Color(fillColorArgb, true))
                .build();
            // Two-arg form (trueValues): real 2px/side insets - the single-arg
            // setInsets(Insets) silently adds (int)(0.18f*size) per side instead.
            icon.setInsets(new Insets(2, 2, 2, 2), true);
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            int d = getIconDepthPx(icon);
            baselineRatioCache.put(sizeKey, icon.getBaseLine());
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
     * Returns the TeXIcon math-baseline ratio for {@code formula} typeset at {@code fontSize}
     * with the given jlatexmath {@code style}, or {@code 0f} if the formula is invalid/failed.
     *
     * <p>
     * The ratio is exactly {@link TeXIcon#getBaseLine()}: the distance from the icon's top edge
     * to its math baseline divided by the icon's total height (the true 2px/side insets
     * included - the icons are built with the two-arg {@code setInsets(insets, true)}, NOT the
     * single-arg variant, which silently adds {@code (int)(0.18f*size)} per side), a
     * value in {@code [0,1]}. It is deliberately kept in this exact float form - NOT rounded via
     * {@code ceil(getTrueIconDepth())} - so consumers can compute the display depth as
     * {@code displayH * (1 - ratio)} with a single rounding at the end, instead of rounding the
     * source depth twice.
     *
     * <p>
     * Safe to call from any thread; does NOT upload any OpenGL texture.
     *
     * @param formula       LaTeX source string
     * @param fillColorArgb ARGB colour (only used for cache key uniformity; does not affect size)
     * @param fontSize      target typeset font size in pixels (body base font size × user scale)
     * @param style         jlatexmath style constant ({@link TeXConstants#STYLE_DISPLAY} or
     *                      {@link TeXConstants#STYLE_TEXT})
     * @return baseline ratio in [0,1], or 0f on parse failure
     */
    public float measureBaselineRatio(String formula, int fillColorArgb, float fontSize, int style) {
        if (formula == null || formula.isEmpty()) {
            return 0f;
        }
        if (GuideLatexTextureCache.INSTANCE.hasFailed(formula)) {
            return 0f;
        }

        String sizeKey = GuideLatexTextureCache.buildSizeCacheKey(formula, fontSize, style);
        Float cached = baselineRatioCache.get(sizeKey);
        if (cached != null) {
            return cached;
        }

        try {
            TeXFormula texFormula = new TeXFormula(formula);
            TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(style)
                .setSize(fontSize)
                .setFGColor(new Color(fillColorArgb, true))
                .build();
            // Two-arg form (trueValues): real 2px/side insets - the single-arg
            // setInsets(Insets) silently adds (int)(0.18f*size) per side instead.
            icon.setInsets(new Insets(2, 2, 2, 2), true);
            float ratio = icon.getBaseLine();
            baselineRatioCache.put(sizeKey, ratio);
            trimBaselineRatioCacheIfNeeded();
            return ratio;
        } catch (ParseException e) {
            GuideDebugLog
                .warnAlways("[GuideNH/LaTeX] Parse error measuring baseline '{}': {}", formula, e.getMessage());
            return 0f;
        } catch (Exception e) {
            GuideDebugLog
                .warnAlways("[GuideNH/LaTeX] Unexpected error measuring baseline '{}': {}", formula, e.getMessage(), e);
            return 0f;
        }
    }

    private void trimBaselineRatioCacheIfNeeded() {
        if (baselineRatioCache.size() <= MAX_BASELINE_RATIO_ENTRIES) {
            return;
        }
        int removeCount = baselineRatioCache.size() - MAX_BASELINE_RATIO_ENTRIES;
        for (String key : baselineRatioCache.keySet()) {
            if (removeCount <= 0) {
                return;
            }
            if (baselineRatioCache.remove(key) != null) {
                removeCount--;
            }
        }
    }

    private static final int MAX_BASELINE_RATIO_ENTRIES = 512;

    // Deferred LaTeX blit tokens.

    /**
     * Render parameters for a formula registered for deferred rasterization.
     */
    private record LatexBlitParams(String formula, int fillColorArgb, float fontSize, int style) {}

    /** Dedupe: (formula,fillColorArgb,fontSize,style) key -> stable negative blit token. */
    private final ConcurrentHashMap<String, Integer> latexBlitTokenByKey = new ConcurrentHashMap<>();
    /** Negative blit token -> render parameters. */
    private final ConcurrentHashMap<Integer, LatexBlitParams> latexBlitParamsByToken = new ConcurrentHashMap<>();
    /** Negative token allocator. GL texture ids are always >= 1, so negative never collides. */
    private final AtomicInteger latexBlitTokenSeq = new AtomicInteger(-1);

    /**
     * Registers {@code formula} for deferred rasterization at the blit-side
     * device scale and returns a stable negative <em>blit token</em> (never a
     * GL texture id). The token is emitted into the {@code BlitTexture}
     * primitive; {@code GuideRenderEngine} resolves the real texture at draw
     * time via {@link #resolveLatexTexture}, where the exact device output
     * scale of the transform stack is known.
     *
     * <p>
     * The mapping is deduplicated by (formula, color, fontSize, style), so
     * calling this every frame for the same formula returns the same token and
     * the registry stays bounded by the distinct-formula count.
     */
    public int registerLatexBlit(String formula, int fillColorArgb, float fontSize, int style) {
        String key = buildBlitKey(formula, fillColorArgb, fontSize, style);
        Integer existing = latexBlitTokenByKey.get(key);
        if (existing != null) {
            return existing;
        }
        int token = latexBlitTokenSeq.getAndDecrement();
        Integer raced = latexBlitTokenByKey.putIfAbsent(key, token);
        if (raced != null) {
            return raced;
        }
        latexBlitParamsByToken.put(token, new LatexBlitParams(formula, fillColorArgb, fontSize, style));
        return token;
    }

    /** Returns {@code true} when {@code texId} is a deferred latex blit token, not a GL texture id. */
    public boolean isLatexBlitToken(int texId) {
        return texId < 0;
    }

    /**
     * Resolves (creating if needed) the real OpenGL texture for a latex blit
     * token at the given device raster scale. Returns
     * {@code [textureId, widthPx, heightPx]} or {@code null} on failure.
     * Must be called from the Minecraft render thread.
     */
    public int[] resolveLatexTexture(int token, float rasterScale) {
        LatexBlitParams p = latexBlitParamsByToken.get(token);
        if (p == null) {
            return null;
        }
        return getOrCreateTexture(p.formula(), p.fillColorArgb(), p.fontSize(), p.style(), rasterScale);
    }

    private static String buildBlitKey(String formula, int fillColorArgb, float fontSize, int style) {
        return GuideLatexTextureCache.buildScaleKey(fontSize) + ':' + style + ':' + fillColorArgb + ':' + formula;
    }

    /**
     * Returns (and caches) the OpenGL texture for {@code formula}, typeset at
     * {@code fontSize} × {@code rasterScale} pixels. Must be called from the
     * Minecraft render thread.
     *
     * <p>
     * <b>Raster scale contract.</b> The texture is rasterized at
     * {@code fontSize × rasterScale} - the real device output scale (in-game
     * {@code DisplayScale.scaleFactor()}; headless the offscreen
     * {@code PushTransform(0,0,scale)} wrap) - while the blit quad keeps the
     * document {@code formulaDisplayW/H} dimensions. Since the quad is scaled
     * by that same factor to device pixels, texture pixels map 1:1 onto device
     * pixels and GL_LINEAR never upscales (no thin-stroke brightness collapse).
     * The cache key includes {@code rasterScale}, so switching GUI / output
     * scale never reuses a texture rasterized for another device resolution.
     *
     * <p>
     * <b>Supersampled rasterization.</b> The texture is internally
     * typeset/rasterized at {@code fontSize × rasterScale ×
     * LATEX_SUPERSAMPLE_S} and then bilinearly downsampled to the exact
     * device-resolution {@code fontSize × rasterScale} icon size (see
     * {@link #LATEX_SUPERSAMPLE_S} for the mechanism and the resolution contract
     * boundary). The final texture dimensions, the 1:1 blit quad geometry and
     * the GL_LINEAR upload are unchanged - only the internal rasterization
     * resolution rises, redistributing sub-pixel stem energy onto the target
     * grid so unhinted CM-math-font stems (2.5-3.1 px) read as full-weight.
     *
     * @param formula       LaTeX source string
     * @param fillColorArgb ARGB colour for the glyph pixels
     * @param fontSize      target typeset font size in pixels (body base font size × user scale)
     * @param style         jlatexmath style constant ({@link TeXConstants#STYLE_DISPLAY} or
     *                      {@link TeXConstants#STYLE_TEXT})
     * @param rasterScale   device output scale: texture pixels = fontSize × rasterScale
     * @return [textureId, widthPx, heightPx] or null on failure
     */
    public int[] getOrCreateTexture(String formula, int fillColorArgb, float fontSize, int style, float rasterScale) {
        if (formula == null || formula.isEmpty()) {
            return null;
        }
        if (GuideLatexTextureCache.INSTANCE.hasFailed(formula)) {
            return null;
        }
        if (!(rasterScale > 0f)) {
            rasterScale = 1f;
        }

        String texKey = GuideLatexTextureCache
            .buildTextureCacheKey(formula, fillColorArgb, fontSize, rasterScale, style);
        int[] cached = GuideLatexTextureCache.INSTANCE.getTexture(texKey);
        if (cached != null) {
            return cached;
        }

        try {
            TeXFormula texFormula = new TeXFormula(formula);

            // Target-size icon: typeset at fontSize × rasterScale exactly as the
            // Device-resolution contract - its pixel dimensions ARE the
            // final texture size (1:1 blit, GL_LINEAR upload untouched). Built
            // only to read the exact target width/height.
            TeXIcon targetIcon = texFormula.new TeXIconBuilder().setStyle(style)
                .setSize(fontSize * rasterScale)
                .setFGColor(new Color(fillColorArgb, true))
                .build();
            // Two-arg form (trueValues): real 2px/side insets - the single-arg
            // setInsets(Insets) silently adds (int)(0.18f*size) per side instead.
            // The insets scale with rasterScale so the texture is an exact
            // rasterScale × scale of the fontSize document icon (whose insets
            // are 2px, see measureSize) - the glyph then maps 1:1 into the
            // document-sized blit quad at the device scale.
            int targetInset = Math.max(1, Math.round(2f * rasterScale));
            targetIcon.setInsets(new Insets(targetInset, targetInset, targetInset, targetInset), true);
            int targetW = targetIcon.getIconWidth();
            int targetH = targetIcon.getIconHeight();

            // Supersampled icon: typeset at fontSize × rasterScale × S with
            // insets scaled ×S (TeXIcon paints its box at (x + insets)/size, so
            // insets are pixel constants that must grow with the size). The
            // supersampled content is a pure S× linear scale of the target
            // content; rendering it at S× and downsampling redistributes the
            // multi-sample coverage of sub-pixel stems back onto the target
            // grid (see LATEX_SUPERSAMPLE_S).
            float ssSize = fontSize * rasterScale * LATEX_SUPERSAMPLE_S;
            int ssInset = Math.max(1, Math.round(2f * rasterScale * LATEX_SUPERSAMPLE_S));
            TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(style)
                .setSize(ssSize)
                .setFGColor(new Color(fillColorArgb, true))
                .build();
            icon.setInsets(new Insets(ssInset, ssInset, ssInset, ssInset), true);
            icon.setForeground(new Color(fillColorArgb, true));

            BufferedImage ssImage = renderToImage(icon, LATEX_EMBOLDEN_EM * ssSize);
            BufferedImage image = downsampleToDevice(ssImage, targetW, targetH, fillColorArgb);
            int w = image.getWidth();
            int h = image.getHeight();

            int textureId = uploadToGL(image, w, h);
            GuideLatexTextureCache.INSTANCE.putTexture(texKey, textureId, w, h);

            // NOTE: deliberately NOT writing the size cache here. The size
            // cache stores the fontSize document dimensions from measureSize;
            // this texture is rasterized at fontSize × rasterScale, so its
            // pixel dimensions differ and must never feed layout geometry.
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
     * @param displayW  rendered display width in GUI units (equals the texture width)
     * @param displayH  rendered display height in GUI units (equals the texture height)
     * @param textureId OpenGL texture ID obtained from {@link #getOrCreateTexture}
     */
    public void renderLatex(int x, int y, int displayW, int displayH, int textureId) {
        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT | GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
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

    private static int getIconDepthPx(TeXIcon icon) {
        return Math.max(0, (int) Math.ceil(icon.getTrueIconDepth()));
    }

    /**
     * Downsample threshold for the coverage-preserving hole fill.
     *
     * <p>
     * A target pixel whose two point-sample phases both land at or below this
     * alpha is eligible for the true-footprint coverage fill in
     * {@link #downsampleToDevice}: its {@code S×S} source cell coverage is
     * consulted and replaces the alpha when it carries substantially more ink.
     * 50 is the visibility boundary the sparse-stroke defect is measured at
     * ("baseline full-bright → final alpha ≤ 50" = degraded); pixels at or
     * below it read as gaps, and only those are touched, so crisp stroke cores
     * (alpha ≫ 50) are never re-weighted.
     */
    private static final int DOWNSAMPLE_FILL_ALPHA = 50;

    /**
     * Minimum footprint coverage (0-255) for the hole fill in
     * {@link #downsampleToDevice} to re-value a pixel. The fill only rescues a
     * gap pixel when its true {@code S×S} cell is at least this much inked on
     * average; faint edge overlaps (coverage 50-70, visually negligible) are
     * left as-is so the fill never adds noise-width strokes.
     */
    private static final int DOWNSAMPLE_FILL_COVERAGE = 70;

    /**
     * Bilinearly downsamples the {@code S×} supersampled ARGB image to the
     * exact device-resolution {@code targetW × targetH} icon size.
     *
     * <p>
     * <b>Dual-phase sampling.</b> The supersampled icon is typeset at
     * exactly {@code S×} the target icon with {@code S×} the insets, so its
     * glyph content sits on an exact {@code S×} lattice of the target content:
     * content belonging to target column {@code dx} occupies source column
     * {@code S·dx}. The legacy center-of-pixel mapping
     * {@code sx = (dx + 0.5) · sw/targetW − 0.5} carries a −0.5 phase offset:
     * at an exact 3:1 ratio it samples source columns {@code 3·dx + 1} and
     * therefore skips every source column of the form {@code 3·dx} entirely - a
     * thin stroke (2.5-3.1 device px ≈ 7.5-9.3 source px wide) whose core sits
     * at {@code 3·dx} can fall completely between two sample points and vanish
     * (measured: the {@code \frac{a+b}{c-d}} "+" stem at column 21 collapses to
     * alpha 0). The fix samples BOTH lattices and takes the max-coverage blend:
     * the legacy grid (crisp single-texel cores at an exact 3:1 ratio, the
     * legacy full-bright behaviour) plus a content-aligned grid
     * ({@code sx = S·dx + 0.5}, which lands ON the {@code S·dx} content columns
     * the legacy grid cannot see). A target pixel is never darker than the
     * strongest phase that finds ink there. The two grids are the same bilinear
     * kernel, only their phase offsets differ; neither is a formula- or
     * column-specific special case, and the ratio mismatch (2.88-3.43, from
     * jlatexmath's {@code (int)(boxW·size + 0.99)} truncation) only decides
     * which phase happens to land closer to each stroke's core.
     *
     * <p>
     * <b>Coverage-preserving hole fill.</b> Where BOTH phases still
     * land in a gap (e.g. content shifted off its nominal {@code S×} lattice by
     * per-glyph rounding, or a stroke thinner than the sample pitch), the pixel
     * is not background - its true {@code S×S} footprint carries ink that the
     * point samples cannot see. Such pixels (alpha ≤ {@link
     * #DOWNSAMPLE_FILL_ALPHA}) are re-valued with the footprint's area coverage
     * (box-filter ratio of inked source area over cell area), so no stroke
     * column is ever fully zeroed and the "baseline full-bright → final ≤ 50"
     * degradation is eliminated. This is the coverage semantics of option ② in
     * that diagnosis: alpha IS coverage, and a pixel whose samples straddle
     * a stroke boundary (or miss it entirely) is valued by the ink actually
     * covering its cell, not by an average that can land at zero.
     *
     * <p>
     * <b>Why bilinear point sampling and not full area averaging.</b> The
     * supersampled icon is <em>not</em> an exact integer {@code S}× scale of
     * the target icon: jlatexmath's pixel dimensions go through
     * {@code (int)(boxW * size + 0.99)} truncation (see {@code TeXIcon}),
     * so the measured size ratio lands between 2.97 and 3.00 (e.g. 196×393
     * over 66×131 = 2.970). Full area averaging was measured and rejected: it
     * dilutes core pixels (a cell of 8 full + 1 edge sample averages below the
     * full-bright threshold), regressing the very full-bright metric this fix
     * exists to restore. The hole fill above applies the area ratio ONLY to
     * pixels that are otherwise zero - cores keep their crisp point-sample
     * value.
     *
     * <p>
     * <b>Alpha-only semantics (with local-colour pass-through).</b> A
     * jlatexmath formula texture is a coverage mask: glyph pixels carry a
     * colour in RGB and the antialiased coverage in alpha (background is
     * transparent black), and the blit blends with GL_SRC_ALPHA /
     * ONE_MINUS_SRC_ALPHA. Downsampling is therefore performed on the
     * <em>alpha channel only</em>: the multi-sample coverage energy of the
     * supersampled grid is redistributed onto the target grid via bilinear
     * interpolation. For the RGB channels, an ink sample within
     * {@link #INK_COLOR_TOLERANCE} of the global fill colour is re-stamped
     * with that constant fill colour - keeping the straight-alpha contract the
     * GL_SRC_ALPHA blend expects (interpolating straight-alpha RGBA directly
     * would average edge pixels against transparent black and darken the RGB
     * at the border, the classic premultiplied-vs-straight fringe). But when a
     * sampled ink pixel clearly differs from the fill colour (a local
     * \texttt{textcolor} subtree ({@code \color} is not a registered macro in jlatexmath-1.0.7),
     * {@code ColorAtom}/{@code ColorBox}
     * {@code setColor}), the RGB is alpha-weighted interpolated from the ink
     * samples instead, so locally coloured glyphs survive the downsample - the
     * same per-glyph colours the old direct-render-and-upload pipeline produced.
     * The footprint fill applies the same rule to its ink samples.
     *
     * @param ssImage       the {@code fontSize × rasterScale × S} supersampled render
     * @param targetW       exact device-resolution width (the icon width at {@code fontSize × rasterScale})
     * @param targetH       exact device-resolution height (the icon height at {@code fontSize × rasterScale})
     * @param fillColorArgb ARGB fill colour (re-stamped onto ink pixels whose RGB is within tolerance of it)
     * @return the downsampled {@code targetW × targetH} ARGB image
     */
    private static BufferedImage downsampleToDevice(BufferedImage ssImage, int targetW, int targetH,
        int fillColorArgb) {
        int sw = ssImage.getWidth();
        int sh = ssImage.getHeight();
        if (sw == targetW && sh == targetH) {
            return ssImage;
        }
        int[] src = new int[sw * sh];
        ssImage.getRGB(0, 0, sw, sh, src, 0, sw);

        int fillRgb = fillColorArgb & 0x00FFFFFF;
        int[] dst = new int[targetW * targetH];
        for (int dy = 0; dy < targetH; dy++) {
            // Phase A - legacy box-ratio sample grid (crisp single-texel cores).
            float syA = clampSamplePos((dy + 0.5f) * sh / targetH - 0.5f, sh);
            int y0A = (int) Math.floor(syA);
            int y1A = Math.min(y0A + 1, sh - 1);
            float fyA = syA - y0A;
            // Phase B - content-aligned grid at the exact S× lattice.
            float syB = clampSamplePos(LATEX_SUPERSAMPLE_S * dy + 0.5f, sh);
            int y0B = (int) Math.floor(syB);
            int y1B = Math.min(y0B + 1, sh - 1);
            float fyB = syB - y0B;
            // True S×S footprint of this target pixel in the content frame.
            float yLo = LATEX_SUPERSAMPLE_S * dy;
            float yHi = yLo + LATEX_SUPERSAMPLE_S;
            for (int dx = 0; dx < targetW; dx++) {
                float sxA = clampSamplePos((dx + 0.5f) * sw / targetW - 0.5f, sw);
                int x0A = (int) Math.floor(sxA);
                int x1A = Math.min(x0A + 1, sw - 1);
                float fxA = sxA - x0A;

                int p00A = src[y0A * sw + x0A];
                int p10A = src[y0A * sw + x1A];
                int p01A = src[y1A * sw + x0A];
                int p11A = src[y1A * sw + x1A];
                float a00A = ((p00A >>> 24) & 0xFF) / 255f;
                float a10A = ((p10A >>> 24) & 0xFF) / 255f;
                float a01A = ((p01A >>> 24) & 0xFF) / 255f;
                float a11A = ((p11A >>> 24) & 0xFF) / 255f;
                float w00A = (1f - fxA) * (1f - fyA);
                float w10A = fxA * (1f - fyA);
                float w01A = (1f - fxA) * fyA;
                float w11A = fxA * fyA;
                float aA = a00A * w00A + a10A * w10A + a01A * w01A + a11A * w11A;

                float sxB = clampSamplePos(LATEX_SUPERSAMPLE_S * dx + 0.5f, sw);
                int x0B = (int) Math.floor(sxB);
                int x1B = Math.min(x0B + 1, sw - 1);
                float fxB = sxB - x0B;

                int p00B = src[y0B * sw + x0B];
                int p10B = src[y0B * sw + x1B];
                int p01B = src[y1B * sw + x0B];
                int p11B = src[y1B * sw + x1B];
                float a00B = ((p00B >>> 24) & 0xFF) / 255f;
                float a10B = ((p10B >>> 24) & 0xFF) / 255f;
                float a01B = ((p01B >>> 24) & 0xFF) / 255f;
                float a11B = ((p11B >>> 24) & 0xFF) / 255f;
                float w00B = (1f - fxB) * (1f - fyB);
                float w10B = fxB * (1f - fyB);
                float w01B = (1f - fxB) * fyB;
                float w11B = fxB * fyB;
                float aB = a00B * w00B + a10B * w10B + a01B * w01B + a11B * w11B;

                // Max-coverage blend: never darker than the strongest phase.
                int ai;
                int rgb;
                if (aB > aA) {
                    ai = Math.round(aB * 255f);
                    rgb = downsampledRgb(p00B, p10B, p01B, p11B, w00B, w10B, w01B, w11B, fillRgb);
                } else {
                    ai = Math.round(aA * 255f);
                    rgb = downsampledRgb(p00A, p10A, p01A, p11A, w00A, w10A, w01A, w11A, fillRgb);
                }
                if (ai < 0) {
                    ai = 0;
                } else if (ai > 255) {
                    ai = 255;
                }

                if (ai <= DOWNSAMPLE_FILL_ALPHA) {
                    // Coverage-preserving hole fill: the true S×S footprint.
                    float xLo = LATEX_SUPERSAMPLE_S * dx;
                    float xHi = xLo + LATEX_SUPERSAMPLE_S;
                    int bx0 = Math.max(0, (int) Math.floor(xLo));
                    int bx1 = Math.min(sw - 1, (int) Math.ceil(xHi) - 1);
                    int by0 = Math.max(0, (int) Math.floor(yLo));
                    int by1 = Math.min(sh - 1, (int) Math.ceil(yHi) - 1);
                    float aSum = 0f;
                    float wSum = 0f;
                    float rAcc = 0f;
                    float gAcc = 0f;
                    float bAcc = 0f;
                    float wCol = 0f;
                    boolean colored = false;
                    for (int syy = by0; syy <= by1; syy++) {
                        float wy = Math.max(0f, Math.min(yHi, syy + 1) - Math.max(yLo, syy));
                        for (int sxx = bx0; sxx <= bx1; sxx++) {
                            float wx = Math.max(0f, Math.min(xHi, sxx + 1) - Math.max(xLo, sxx));
                            float w = wx * wy;
                            int p = src[syy * sw + sxx];
                            int pa = (p >>> 24) & 0xFF;
                            aSum += w * pa;
                            wSum += w;
                            if (pa > 0) {
                                int pr = (p >> 16) & 0xFF;
                                int pg = (p >> 8) & 0xFF;
                                int pb = p & 0xFF;
                                if (Math.abs(pr - ((fillRgb >> 16) & 0xFF)) > INK_COLOR_TOLERANCE
                                    || Math.abs(pg - ((fillRgb >> 8) & 0xFF)) > INK_COLOR_TOLERANCE
                                    || Math.abs(pb - (fillRgb & 0xFF)) > INK_COLOR_TOLERANCE) {
                                    colored = true;
                                }
                                float ww = w * pa;
                                rAcc += ww * pr;
                                gAcc += ww * pg;
                                bAcc += ww * pb;
                                wCol += ww;
                            }
                        }
                    }
                    if (wSum > 0f) {
                        int bai = Math.round(aSum / wSum);
                        if (bai > ai && bai >= DOWNSAMPLE_FILL_COVERAGE) {
                            ai = bai;
                            if (!colored || wCol <= 0f) {
                                rgb = fillRgb;
                            } else {
                                rgb = (Math.round(rAcc / wCol) << 16) | (Math.round(gAcc / wCol) << 8)
                                    | Math.round(bAcc / wCol);
                            }
                        }
                    }
                }
                dst[dy * targetW + dx] = (ai << 24) | rgb;
            }
        }

        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, targetW, targetH, dst, 0, targetW);
        return out;
    }

    /**
     * Clamps a fractional source sample position into {@code [0, size - 1]} so a
     * phase offset at the image border can never produce a negative or
     * out-of-range texel index or a negative interpolation weight.
     */
    private static float clampSamplePos(float pos, int size) {
        if (pos < 0f) {
            return 0f;
        }
        if (pos > size - 1f) {
            return size - 1f;
        }
        return pos;
    }

    /**
     * Per-channel colour tolerance (0-255 scale) for {@link #downsampleToDevice}:
     * an ink sample whose RGB is within this many units of the global fill
     * colour in every channel is treated as uncoloured and re-stamped with the
     * fill colour; a sample differing by more is a local
     * \texttt{textcolor} colour that must survive downsampling.
     * Java2D paints glyph RGBs exactly (no channel blending happens inside the
     * fill/stroke), so uncoloured ink pixels match the fill colour bit-for-bit
     * and the tolerance only keeps the fast re-stamp path strict while a local
     * colour almost identical to the fill colour (visually indistinguishable
     * anyway) is not misclassified.
     */
    private static final int INK_COLOR_TOLERANCE = 8;

    /**
     * Decides the RGB of one downsampled pixel from its four bilinear samples.
     * Transparent background samples ({@code alpha == 0}) carry a meaningless
     * RGB and are excluded so they cannot darken the mix (the straight-alpha
     * fringe the re-stamp path exists to avoid). When every ink sample is
     * within {@link #INK_COLOR_TOLERANCE} of {@code fillRgb} the constant fill
     * colour is re-stamped (historical behaviour); when at least one ink sample
     * clearly differs (local \texttt{textcolor} subtree) the RGB is alpha-weighted
     * bilinear interpolation of the ink samples, preserving locally coloured
     * glyphs through the downsample.
     */
    private static int downsampledRgb(int p00, int p10, int p01, int p11, float w00, float w10, float w01, float w11,
        int fillRgb) {
        int fillR = (fillRgb >> 16) & 0xFF;
        int fillG = (fillRgb >> 8) & 0xFF;
        int fillB = fillRgb & 0xFF;
        int[] ps = { p00, p10, p01, p11 };
        float[] ws = { w00, w10, w01, w11 };
        float rAcc = 0f;
        float gAcc = 0f;
        float bAcc = 0f;
        float wAcc = 0f;
        boolean colored = false;
        for (int i = 0; i < 4; i++) {
            int p = ps[i];
            int pa = (p >>> 24) & 0xFF;
            if (pa == 0) {
                continue;
            }
            int pr = (p >> 16) & 0xFF;
            int pg = (p >> 8) & 0xFF;
            int pb = p & 0xFF;
            if (Math.abs(pr - fillR) > INK_COLOR_TOLERANCE || Math.abs(pg - fillG) > INK_COLOR_TOLERANCE
                || Math.abs(pb - fillB) > INK_COLOR_TOLERANCE) {
                colored = true;
            }
            float w = ws[i] * pa;
            rAcc += w * pr;
            gAcc += w * pg;
            bAcc += w * pb;
            wAcc += w;
        }
        if (!colored || wAcc <= 0f) {
            return fillRgb;
        }
        return (Math.round(rAcc / wAcc) << 16) | (Math.round(gAcc / wAcc) << 8) | Math.round(bAcc / wAcc);
    }

    private BufferedImage renderToImage(TeXIcon icon, float strokePx) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g.setColor(new Color(ColorUtils.TRANSPARENT.getColor(), true));
            g.fillRect(0, 0, w, h);

            // Outline-domain emboldening (MathJax "blacker" equivalent): wrap the
            // Graphics2D so every glyph jlatexmath draws via drawChars (CharBox.draw)
            // is rendered as its GlyphVector outline - fill + same-colour stroke -
            // instead of plain Java2D text (which cannot be stroked). strokePx is the
            // desired stroke width in pixels of THIS supersampled image
            // (= LATEX_EMBOLDEN_EM × fontSize × rasterScale × S, see the constant).
            // strokePx <= 0 keeps the plain drawChars path (disabled emboldening).
            Graphics2D paintTarget = (strokePx > 0f) ? new EmboldenGraphics2D(g, strokePx) : g;
            icon.paintIcon(null, paintTarget, 0, 0);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Delegating {@link Graphics2D} that converts text drawing into glyph-outline
     * fill + same-colour stroke - the Java2D equivalent of the MathJax SVG
     * {@code blacker} mechanism (see {@link #LATEX_EMBOLDEN_EM}).
     *
     * <p>
     * Java2D cannot stroke text rendered through {@code drawChars} (the only
     * text path jlatexmath uses - {@code CharBox.draw}), so this wrapper
     * intercepts {@link #drawChars} / {@code drawString} / {@link #drawGlyphVector}
     * and re-renders each glyph as {@code GlyphVector.getOutline(x, y)}: the
     * outline is filled with the current colour (byte-identical to {@code drawChars}
     * at stroke width 0 - probe-verified, diffPx=0) and then stroked with a
     * {@link BasicStroke} of the requested width. All other Graphics2D operations
     * (fraction bars are {@code fill(Rectangle2D)}, boxes are shape draws) are
     * delegated untouched, so only glyph strokes are emboldened - matching MathJax,
     * whose blacker CSS targets {@code path[data-c]} glyph paths only.
     *
     * <p>
     * <b>Stroke width in user space (constant device-pixel stroke).</b>
     * {@code strokePx} is in device pixels of the supersampled image, so at draw
     * time the BasicStroke is requested in user-space units as
     * {@code strokePx / |current transform scale|} ({@code strokeUserWidth()})
     * and the CTM maps it back to a device width of exactly {@code strokePx}.
     * The current transform is CharBox's {@code scale(size/100)} (TeXIcon's
     * global {@code scale(size)} already applied outside it), and that per-glyph
     * scale carries the script size factor
     * ({@code DefaultTeXFont.getSizeFactor()}: script 0.7, script-script 0.5),
     * so dividing it out cancels it: the stroked device width is the SAME
     * {@code strokePx} for normal, script and script-script glyphs. Script
     * glyphs therefore do NOT get an em-proportional (thinner) stroke - on the
     * contrary they end up ~1/0.7 heavier relative to their own reduced em.
     *
     * <p>
     * This deliberately differs from MathJax, whose SVG {@code blacker}
     * (3/1000em) is em-relative per glyph: MathJax glyph paths are stroked in
     * their own em coordinate space, so script glyphs receive a proportionally
     * thinner stroke. We keep the constant device-pixel width because (a) script
     * glyphs have the thinnest stems and need the full stroke for readability
     * (readability over strict metric equivalence - adjudicated decision G);
     * (b) METAFONT's blacker is likewise not an em-ratio adjustment per size
     * (it is a fixed offset applied at font-build time); and (c) the entire
     * probe calibration (s=2 corpus ≥0.70 full-bright, glyph counters not
     * closed) was measured under this constant-device-pixel behaviour - moving
     * to per-glyph em-ratio strokes would require re-calibrating everything and
     * would make script glyphs thinner at exactly the size where unhinted thin
     * stems are hardest to keep readable.
     */
    private static final class EmboldenGraphics2D extends Graphics2D {

        private final Graphics2D delegate;
        private final float strokePx;

        private EmboldenGraphics2D(Graphics2D delegate, float strokePx) {
            this.delegate = delegate;
            this.strokePx = strokePx;
        }

        private float strokeUserWidth() {
            AffineTransform t = delegate.getTransform();
            double scale = Math.hypot(t.getScaleX(), t.getShearY());
            if (scale <= 0d) {
                scale = 1d;
            }
            return strokePx / (float) scale;
        }

        private void drawGlyphsAsOutlines(String text, float x, float y) {
            if (text.isEmpty()) {
                return;
            }
            java.awt.Font font = delegate.getFont();
            GlyphVector gv = font.createGlyphVector(delegate.getFontRenderContext(), text);
            Shape outline = gv.getOutline(x, y);
            delegate.fill(outline);
            float width = strokeUserWidth();
            if (width > 0f) {
                Stroke old = delegate.getStroke();
                delegate.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                delegate.draw(outline);
                delegate.setStroke(old);
            }
        }

        @Override
        public void drawChars(char[] data, int offset, int length, int x, int y) {
            drawGlyphsAsOutlines(new String(data, offset, length), x, y);
        }

        @Override
        public void drawString(String str, int x, int y) {
            drawGlyphsAsOutlines(str, x, y);
        }

        @Override
        public void drawString(String str, float x, float y) {
            drawGlyphsAsOutlines(str, x, y);
        }

        @Override
        public void drawString(AttributedCharacterIterator iterator, int x, int y) {
            delegate.drawString(iterator, x, y);
        }

        @Override
        public void drawString(AttributedCharacterIterator iterator, float x, float y) {
            delegate.drawString(iterator, x, y);
        }

        @Override
        public void drawGlyphVector(GlyphVector g, float x, float y) {
            Shape outline = g.getOutline(x, y);
            delegate.fill(outline);
            float width = strokeUserWidth();
            if (width > 0f) {
                Stroke old = delegate.getStroke();
                delegate.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                delegate.draw(outline);
                delegate.setStroke(old);
            }
        }

        // Every drawing operation other than text is pure delegation.

        @Override
        public void draw(Shape s) {
            delegate.draw(s);
        }

        @Override
        public boolean drawImage(java.awt.Image img, AffineTransform xform, java.awt.image.ImageObserver obs) {
            return delegate.drawImage(img, xform, obs);
        }

        @Override
        public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
            delegate.drawImage(img, op, x, y);
        }

        @Override
        public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
            delegate.drawRenderedImage(img, xform);
        }

        @Override
        public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
            delegate.drawRenderableImage(img, xform);
        }

        @Override
        public void fill(Shape s) {
            delegate.fill(s);
        }

        @Override
        public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
            return delegate.hit(rect, s, onStroke);
        }

        @Override
        public GraphicsConfiguration getDeviceConfiguration() {
            return delegate.getDeviceConfiguration();
        }

        @Override
        public void setComposite(java.awt.Composite comp) {
            delegate.setComposite(comp);
        }

        @Override
        public void setPaint(Paint paint) {
            delegate.setPaint(paint);
        }

        @Override
        public void setStroke(Stroke s) {
            delegate.setStroke(s);
        }

        @Override
        public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
            delegate.setRenderingHint(hintKey, hintValue);
        }

        @Override
        public Object getRenderingHint(RenderingHints.Key hintKey) {
            return delegate.getRenderingHint(hintKey);
        }

        @Override
        public void setRenderingHints(java.util.Map<?, ?> hints) {
            delegate.setRenderingHints(hints);
        }

        @Override
        public void addRenderingHints(java.util.Map<?, ?> hints) {
            delegate.addRenderingHints(hints);
        }

        @Override
        public RenderingHints getRenderingHints() {
            return delegate.getRenderingHints();
        }

        @Override
        public void translate(int x, int y) {
            delegate.translate(x, y);
        }

        @Override
        public void translate(double tx, double ty) {
            delegate.translate(tx, ty);
        }

        @Override
        public void rotate(double theta) {
            delegate.rotate(theta);
        }

        @Override
        public void rotate(double theta, double x, double y) {
            delegate.rotate(theta, x, y);
        }

        @Override
        public void scale(double sx, double sy) {
            delegate.scale(sx, sy);
        }

        @Override
        public void shear(double shx, double shy) {
            delegate.shear(shx, shy);
        }

        @Override
        public void transform(AffineTransform Tx) {
            delegate.transform(Tx);
        }

        @Override
        public void setTransform(AffineTransform Tx) {
            delegate.setTransform(Tx);
        }

        @Override
        public AffineTransform getTransform() {
            return delegate.getTransform();
        }

        @Override
        public Paint getPaint() {
            return delegate.getPaint();
        }

        @Override
        public java.awt.Composite getComposite() {
            return delegate.getComposite();
        }

        @Override
        public void setBackground(Color color) {
            delegate.setBackground(color);
        }

        @Override
        public Color getBackground() {
            return delegate.getBackground();
        }

        @Override
        public Stroke getStroke() {
            return delegate.getStroke();
        }

        @Override
        public void clip(Shape s) {
            delegate.clip(s);
        }

        @Override
        public java.awt.font.FontRenderContext getFontRenderContext() {
            return delegate.getFontRenderContext();
        }

        // Straight Graphics delegation.

        @Override
        public Graphics create() {
            return delegate.create();
        }

        @Override
        public Color getColor() {
            return delegate.getColor();
        }

        @Override
        public void setColor(Color c) {
            delegate.setColor(c);
        }

        @Override
        public void setPaintMode() {
            delegate.setPaintMode();
        }

        @Override
        public void setXORMode(Color c1) {
            delegate.setXORMode(c1);
        }

        @Override
        public java.awt.Font getFont() {
            return delegate.getFont();
        }

        @Override
        public void setFont(java.awt.Font font) {
            delegate.setFont(font);
        }

        @Override
        public FontMetrics getFontMetrics(java.awt.Font f) {
            return delegate.getFontMetrics(f);
        }

        @Override
        public Rectangle getClipBounds() {
            return delegate.getClipBounds();
        }

        @Override
        public void clipRect(int x, int y, int width, int height) {
            delegate.clipRect(x, y, width, height);
        }

        @Override
        public void setClip(int x, int y, int width, int height) {
            delegate.setClip(x, y, width, height);
        }

        @Override
        public Shape getClip() {
            return delegate.getClip();
        }

        @Override
        public void setClip(Shape clip) {
            delegate.setClip(clip);
        }

        @Override
        public void copyArea(int x, int y, int width, int height, int dx, int dy) {
            delegate.copyArea(x, y, width, height, dx, dy);
        }

        @Override
        public void drawLine(int x1, int y1, int x2, int y2) {
            delegate.drawLine(x1, y1, x2, y2);
        }

        @Override
        public void fillRect(int x, int y, int width, int height) {
            delegate.fillRect(x, y, width, height);
        }

        @Override
        public void clearRect(int x, int y, int width, int height) {
            delegate.clearRect(x, y, width, height);
        }

        @Override
        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }

        @Override
        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }

        @Override
        public void drawOval(int x, int y, int width, int height) {
            delegate.drawOval(x, y, width, height);
        }

        @Override
        public void fillOval(int x, int y, int width, int height) {
            delegate.fillOval(x, y, width, height);
        }

        @Override
        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.drawArc(x, y, width, height, startAngle, arcAngle);
        }

        @Override
        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.fillArc(x, y, width, height, startAngle, arcAngle);
        }

        @Override
        public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolyline(xPoints, yPoints, nPoints);
        }

        @Override
        public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolygon(xPoints, yPoints, nPoints);
        }

        @Override
        public void drawPolygon(Polygon p) {
            delegate.drawPolygon(p);
        }

        @Override
        public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.fillPolygon(xPoints, yPoints, nPoints);
        }

        @Override
        public void fillPolygon(Polygon p) {
            delegate.fillPolygon(p);
        }

        @Override
        public boolean drawImage(java.awt.Image img, int x, int y, java.awt.image.ImageObserver observer) {
            return delegate.drawImage(img, x, y, observer);
        }

        @Override
        public boolean drawImage(java.awt.Image img, int x, int y, int width, int height,
            java.awt.image.ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, observer);
        }

        @Override
        public boolean drawImage(java.awt.Image img, int x, int y, Color bgcolor,
            java.awt.image.ImageObserver observer) {
            return delegate.drawImage(img, x, y, bgcolor, observer);
        }

        @Override
        public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, Color bgcolor,
            java.awt.image.ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
        }

        @Override
        public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
            int sy2, java.awt.image.ImageObserver observer) {
            return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
        }

        @Override
        public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
            int sy2, Color bgcolor, java.awt.image.ImageObserver observer) {
            return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }
    }

    private static int uploadToGL(BufferedImage image, int w, int h) {
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);

        ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }
}
