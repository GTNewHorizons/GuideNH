package com.hfstudio.guidenh.integration.angelica;

import com.gtnewhorizons.angelica.config.FontConfig;

import cpw.mods.fml.common.Optional;

public class AngelicaFontSupport {

    @Optional.Method(modid = "angelica")
    public static float yScaleMultiplier() {
        if (!FontConfig.enableCustomFont) {
            return 1f;
        }
        float scale = FontConfig.customFontScale;
        return scale > 0f && Float.isFinite(scale) ? scale : 1f;
    }
}
