package com.hfstudio.guidenh.integration.mui2.capture;

import net.minecraft.inventory.Container;

import com.cleanroommc.modularui.screen.ModularContainer;

public class Mui2ClientContainerReadiness {

    private Mui2ClientContainerReadiness() {}

    public static boolean ready(Container container) {
        return !(container instanceof ModularContainer modular)
            || modular.isInitialized() && modular.getScreen() != null;
    }
}
