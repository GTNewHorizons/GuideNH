package com.hfstudio.guidenh.client.capture.geometry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureCoordinateMapper {

    private CaptureCoordinateMapper() {}

    public static CaptureViewport current(Minecraft minecraft) {
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        return new CaptureViewport(
            resolution.getScaledWidth(),
            resolution.getScaledHeight(),
            minecraft.displayWidth,
            minecraft.displayHeight,
            resolution.getScaleFactor(),
            resolution.getScaleFactor());
    }
}
