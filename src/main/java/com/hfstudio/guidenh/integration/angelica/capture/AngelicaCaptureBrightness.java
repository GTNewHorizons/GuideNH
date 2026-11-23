package com.hfstudio.guidenh.integration.angelica.capture;

import net.minecraft.client.renderer.OpenGlHelper;

import com.gtnewhorizons.angelica.glsm.GLSMCurrentAttribs;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMHooks;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;

public class AngelicaCaptureBrightness {

    private static boolean initialized;

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        GLSMHooks.LIGHTMAP_COORDS.addListener(event -> {
            if (!CaptureRuntime.instance()
                .fullBrightness() || event.x == 240 && event.y == 240) return;
            // Re-enter the setter so earlier listeners receive the corrected coordinates.
            GLSMCurrentAttribs.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        });
    }
}
