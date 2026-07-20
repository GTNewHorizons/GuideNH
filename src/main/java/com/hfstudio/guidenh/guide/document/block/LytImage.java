package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiAssets;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.sound.GuideSoundPlayback;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Getter;
import lombok.Setter;

public class LytImage extends LytBlock implements InteractiveElement {

    public static final double DEFAULT_LAYOUT_SCALE = 0.25d;

    @Getter
    private ResourceLocation imageId;
    @Getter
    private GuidePageTexture texture = GuidePageTexture.missing();
    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private String alt;

    private int explicitWidth = -1;
    private int explicitHeight = -1;
    private int cropX;
    private int cropY;
    private int cropWidth = -1;
    private int cropHeight = -1;
    private double scaleX = 1.0d;
    private double scaleY = 1.0d;

    @Getter
    private final List<ImageRegionAnnotation> annotations = new ArrayList<>();
    @Nullable
    private ImageRegionAnnotation hoveredSoundAnnotation;

    public void setImage(ResourceLocation id, byte @Nullable [] imageData) {
        this.imageId = id;
        if (imageData != null) {
            this.texture = GuidePageTexture.load(id, imageData);
        } else {
            this.texture = GuidePageTexture.missing();
        }
    }

    public void setTexture(@Nullable ResourceLocation id, @Nullable GuidePageTexture texture) {
        this.imageId = id;
        this.texture = texture != null ? texture : GuidePageTexture.missing();
    }

    public void setExplicitWidth(int width) {
        this.explicitWidth = width > 0 ? width : -1;
    }

    public void setExplicitHeight(int height) {
        this.explicitHeight = height > 0 ? height : -1;
    }

    public void setCropRect(int cropX, int cropY, int cropWidth, int cropHeight) {
        this.cropX = Math.max(0, cropX);
        this.cropY = Math.max(0, cropY);
        this.cropWidth = cropWidth > 0 ? cropWidth : -1;
        this.cropHeight = cropHeight > 0 ? cropHeight : -1;
    }

    public void setScale(double scaleX, double scaleY) {
        this.scaleX = scaleX > 0.0d ? scaleX : 1.0d;
        this.scaleY = scaleY > 0.0d ? scaleY : 1.0d;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (texture == null) {
            return new LytRect(x, y, 32, 32);
        }

        var size = texture.getSize();
        int sourceWidth = Math.max(1, cropWidth > 0 ? cropWidth : size.width());
        int sourceHeight = Math.max(1, cropHeight > 0 ? cropHeight : size.height());
        int width;
        int height;
        if (explicitWidth > 0 || explicitHeight > 0) {
            width = explicitWidth > 0 ? explicitWidth : Math.max(1, (int) Math.round(sourceWidth * scaleX));
            height = explicitHeight > 0 ? explicitHeight : Math.max(1, (int) Math.round(sourceHeight * scaleY));
        } else {
            width = Math.max(1, (int) Math.round(sourceWidth * DEFAULT_LAYOUT_SCALE * scaleX));
            height = Math.max(1, (int) Math.round(sourceHeight * DEFAULT_LAYOUT_SCALE * scaleY));
        }

        float visualScale = context.getVisualScale();
        if (visualScale < 0.999f) {
            width = Math.max(1, Math.round(width * visualScale));
            height = Math.max(1, Math.round(height * visualScale));
        }

        if (width > availableWidth) {
            var f = availableWidth / (float) width;
            width = Math.max(1, Math.round(width * f));
            height = Math.max(1, Math.round(height * f));
        }

        return new LytRect(x, y, width, height);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void onMouseLeave() {
        hoveredSoundAnnotation = null;
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        var bounds = getBounds();
        if (texture == null || texture.isMissing()) {
            // Fall back to missing texture sprite
            emitBlitGuiSprite(c, GuiAssets.MISSING_TEXTURE, bounds.x(), bounds.y(), bounds.width(), bounds.height());
        } else {
            ResourceLocation resolvedTex = texture.getTexture();
            int texId = resolvedTex != null ? getGlTextureId(resolvedTex) : -1;
            if (texId >= 0) {
                // Full texture UV — matches the legacy fillTexturedRect behavior
                c.emit(
                    new GuideRenderPrimitive.BlitTexture(
                        texId,
                        bounds.x(),
                        bounds.y(),
                        bounds.width(),
                        bounds.height(),
                        0f,
                        0f,
                        1f,
                        1f));
            } else {
                // Texture object not (yet) registered with the TextureManager —
                // fall back to the missing-texture sprite instead of leaving an
                // empty box.
                emitBlitGuiSprite(
                    c,
                    GuiAssets.MISSING_TEXTURE,
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height());
            }
        }
    }

    @Override
    public void emitDecorations(PrimitiveCollector c) {
        if (annotations.isEmpty()) {
            return;
        }
        var bounds = getBounds();
        int dispW = bounds.width();
        int dispH = bounds.height();
        if (dispW <= 0 || dispH <= 0) {
            return;
        }
        int natW = texture != null && !texture.isMissing() ? getEffectiveSourceWidth() : dispW;
        int natH = texture != null && !texture.isMissing() ? getEffectiveSourceHeight() : dispH;
        for (var ann : annotations) {
            if (!ann.isShowBorder()) {
                continue;
            }
            int bx;
            int by;
            int bw;
            int bh;
            if (ann.isWholeImage()) {
                bx = bounds.x();
                by = bounds.y();
                bw = bounds.width();
                bh = bounds.height();
            } else {
                int clampedX = Math.clamp(ann.getImgX(), 0, natW);
                int clampedY = Math.clamp(ann.getImgY(), 0, natH);
                int clampedW = Math.min(ann.getImgX() + ann.getImgW(), natW) - clampedX;
                int clampedH = Math.min(ann.getImgY() + ann.getImgH(), natH) - clampedY;
                if (clampedW <= 0 || clampedH <= 0) {
                    continue;
                }
                bx = bounds.x() + clampedX * dispW / natW;
                by = bounds.y() + clampedY * dispH / natH;
                bw = Math.max(1, clampedW * dispW / natW);
                bh = Math.max(1, clampedH * dispH / natH);
            }
            int borderArgb = ann.getBorderColor()
                .resolve(LightDarkMode.current());
            c.emit(
                new GuideRenderPrimitive.DrawBorder(
                    bx,
                    by,
                    bw,
                    bh,
                    ann.getBorderThickness(),
                    ann.getBorderThickness(),
                    ann.getBorderThickness(),
                    ann.getBorderThickness(),
                    borderArgb));
        }
    }

    @Override
    public void render(RenderContext context) {
        if (texture == null) {
            context.fillIcon(getBounds(), GuiAssets.MISSING_TEXTURE);
        } else {
            context.fillTexturedRect(
                getBounds(),
                texture,
                cropX,
                cropY,
                getEffectiveSourceWidth(),
                getEffectiveSourceHeight());
        }
        drawAnnotationBorders(context);
    }

    private void drawAnnotationBorders(RenderContext context) {
        if (annotations.isEmpty()) {
            return;
        }
        var bounds = getBounds();
        int dispW = bounds.width();
        int dispH = bounds.height();
        if (dispW <= 0 || dispH <= 0) {
            return;
        }
        int natW = texture != null && !texture.isMissing() ? getEffectiveSourceWidth() : dispW;
        int natH = texture != null && !texture.isMissing() ? getEffectiveSourceHeight() : dispH;
        for (var ann : annotations) {
            if (!ann.isShowBorder()) {
                continue;
            }
            int bx;
            int by;
            int bw;
            int bh;
            if (ann.isWholeImage()) {
                bx = bounds.x();
                by = bounds.y();
                bw = bounds.width();
                bh = bounds.height();
            } else {
                // Clamp the annotation region to [0, natW] x [0, natH] so the border
                // cannot extend beyond the displayed image area regardless of scaling.
                int clampedX = Math.clamp(ann.getImgX(), 0, natW);
                int clampedY = Math.clamp(ann.getImgY(), 0, natH);
                int clampedW = Math.min(ann.getImgX() + ann.getImgW(), natW) - clampedX;
                int clampedH = Math.min(ann.getImgY() + ann.getImgH(), natH) - clampedY;
                if (clampedW <= 0 || clampedH <= 0) {
                    continue;
                }
                bx = bounds.x() + clampedX * dispW / natW;
                by = bounds.y() + clampedY * dispH / natH;
                bw = Math.max(1, clampedW * dispW / natW);
                bh = Math.max(1, clampedH * dispH / natH);
            }
            context.drawBorder(bx, by, bw, bh, context.resolveColor(ann.getBorderColor()), ann.getBorderThickness());
        }
    }

    /**
     * Adds a region annotation to this image. Annotations are tested in reverse insertion order
     * (last-added wins) when querying the tooltip for a given cursor position.
     */
    public void addAnnotation(ImageRegionAnnotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        playHoverSound(x, y);
        if (annotations.isEmpty()) {
            return Optional.empty();
        }
        var bounds = getBounds();
        if (texture == null || texture.isMissing()) {
            return Optional.empty();
        }
        int dispW = bounds.width();
        int dispH = bounds.height();
        if (dispW <= 0 || dispH <= 0) {
            return Optional.empty();
        }
        int natW = getEffectiveSourceWidth();
        int natH = getEffectiveSourceHeight();
        float localX = x - bounds.x();
        float localY = y - bounds.y();
        float imgPx = localX * natW / dispW;
        float imgPy = localY * natH / dispH;
        for (int i = annotations.size() - 1; i >= 0; i--) {
            var ann = annotations.get(i);
            if (ann.getTooltip() == null) {
                continue;
            }
            if (ann.containsImagePoint(imgPx, imgPy)) {
                return Optional.of(ann.getTooltip());
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) {
            return false;
        }
        ImageRegionAnnotation annotation = findSoundAnnotation(x, y, GuideSoundTrigger.CLICK);
        if (annotation == null) {
            return false;
        }
        return GuideSoundPlayback.play(annotation.getSound());
    }

    private void playHoverSound(float x, float y) {
        ImageRegionAnnotation annotation = findSoundAnnotation(x, y, GuideSoundTrigger.HOVER);
        if (annotation == hoveredSoundAnnotation) {
            return;
        }
        hoveredSoundAnnotation = annotation;
        if (annotation != null) {
            GuideSoundPlayback.play(annotation.getSound());
        }
    }

    @Nullable
    private ImageRegionAnnotation findSoundAnnotation(float x, float y, GuideSoundTrigger trigger) {
        if (annotations.isEmpty()) {
            return null;
        }
        ImagePoint point = toImagePoint(x, y);
        if (point == null) {
            return null;
        }
        for (int i = annotations.size() - 1; i >= 0; i--) {
            ImageRegionAnnotation annotation = annotations.get(i);
            GuideSoundSpec sound = annotation.getSound();
            if (sound == null || annotation.getSoundTrigger() != trigger) {
                continue;
            }
            if (annotation.containsImagePoint(point.x, point.y)) {
                return annotation;
            }
        }
        return null;
    }

    @Nullable
    private ImagePoint toImagePoint(float x, float y) {
        var bounds = getBounds();
        if (texture == null || texture.isMissing()) {
            return null;
        }
        int dispW = bounds.width();
        int dispH = bounds.height();
        if (dispW <= 0 || dispH <= 0
            || x < bounds.x()
            || x >= bounds.right()
            || y < bounds.y()
            || y >= bounds.bottom()) {
            return null;
        }
        int natW = getEffectiveSourceWidth();
        int natH = getEffectiveSourceHeight();
        float localX = x - bounds.x();
        float localY = y - bounds.y();
        return new ImagePoint(localX * natW / dispW, localY * natH / dispH);
    }

    private int getEffectiveSourceWidth() {
        if (cropWidth > 0) {
            return cropWidth;
        }
        return texture != null ? Math.max(
            1,
            texture.getSize()
                .width())
            : 1;
    }

    private int getEffectiveSourceHeight() {
        if (cropHeight > 0) {
            return cropHeight;
        }
        return texture != null ? Math.max(
            1,
            texture.getSize()
                .height())
            : 1;
    }

    /**
     * Convert a Minecraft ResourceLocation to a GL texture ID for use with BlitTexture.
     */
    private static int getGlTextureId(ResourceLocation res) {
        try {
            ITextureObject tex = Minecraft.getMinecraft()
                .getTextureManager()
                .getTexture(res);
            return tex != null ? tex.getGlTextureId() : -1;
        } catch (Throwable t) {
            // Headless (unit tests) or texture unavailable: skip drawing.
            return -1;
        }
    }

    /**
     * Emit a BlitTexture for a GuiSprite at the given screen coordinates.
     */
    private static void emitBlitGuiSprite(PrimitiveCollector c, GuiSprite sprite, int x, int y, int w, int h) {
        int texId = getGlTextureId(sprite.getTexture());
        if (texId < 0) return;
        float u = (float) sprite.getU() / sprite.getTexWidth();
        float v = (float) sprite.getV() / sprite.getTexHeight();
        float u2 = (float) (sprite.getU() + sprite.getWidth()) / sprite.getTexWidth();
        float v2 = (float) (sprite.getV() + sprite.getHeight()) / sprite.getTexHeight();
        c.emit(new GuideRenderPrimitive.BlitTexture(texId, x, y, w, h, u, v, u2, v2));
    }

    public static class ImagePoint {

        public final float x;
        public final float y;

        public ImagePoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
