package com.hfstudio.guidenh.guide.scene.annotation;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

import lombok.Getter;

@Getter
public abstract class OverlayAnnotation extends SceneAnnotation {

    /**
     * Fade multiplier in {@code [0, 1]} applied to rendered alpha.
     */
    private float fade = 1f;

    /**
     * Sets the fade multiplier applied to all rendered alpha values.
     * {@code 0} = fully transparent, {@code 1} = fully opaque.
     * Clamped to {@code [0, 1]}.
     */
    public void setFade(float fade) {
        this.fade = Math.clamp(fade, 0f, 1f);
    }

    /**
     * Multiplies an ARGB alpha channel by the fade multiplier.
     */
    protected static int applyFade(int argb, float fade) {
        int a = (int) (((argb >>> 24) & 0xFF) * fade);
        return (Math.min(255, a) << 24) | (argb & 0x00FFFFFF);
    }

    public abstract LytRect getBoundingRect(CameraSettings camera, LytRect viewport);

    public abstract void render(CameraSettings camera, RenderContext context, LytRect viewport);
}
