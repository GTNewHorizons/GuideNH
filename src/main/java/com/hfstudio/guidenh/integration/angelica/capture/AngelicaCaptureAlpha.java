package com.hfstudio.guidenh.integration.angelica.capture;

import com.gtnewhorizons.angelica.glsm.GLSMBlend;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMHooks;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.render.CaptureAlphaBlend;

public class AngelicaCaptureAlpha {

    private static boolean initialized;

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        GLSMHooks.BLEND_FUNC_CHANGE.addListener(event -> {
            if (!CaptureRuntime.instance()
                .capturingGuiLayer()) return;
            int sourceAlpha = CaptureAlphaBlend.sourceAlpha(event.srcRgb, event.dstRgb, event.srcAlpha, event.dstAlpha);
            int destinationAlpha = CaptureAlphaBlend
                .destinationAlpha(event.srcRgb, event.dstRgb, event.srcAlpha, event.dstAlpha);
            if (sourceAlpha == event.srcAlpha && destinationAlpha == event.dstAlpha) return;
            // Re-enter the public setter to update both backend state and its cache.
            GLSMBlend.tryBlendFuncSeparate(event.srcRgb, event.dstRgb, sourceAlpha, destinationAlpha);
        });
    }
}
