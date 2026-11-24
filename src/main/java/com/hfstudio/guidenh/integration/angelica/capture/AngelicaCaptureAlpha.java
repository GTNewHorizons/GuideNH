package com.hfstudio.guidenh.integration.angelica.capture;

import org.lwjgl.opengl.GL11;

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
                .capturingGuiLayer() || !CaptureAlphaBlend.sourceOver(event.srcRgb, event.dstRgb)
                || event.srcAlpha == GL11.GL_ONE && event.dstAlpha == GL11.GL_ONE_MINUS_SRC_ALPHA) return;
            // Re-enter the public setter to update both backend state and its cache.
            GLSMBlend.tryBlendFuncSeparate(event.srcRgb, event.dstRgb, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        });
    }
}
