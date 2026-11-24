package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;

public class CapturePixelConverter {

    private CapturePixelConverter() {}

    public static void normalize(int[] pixels, CaptureAlphaMode alphaMode) {
        if (alphaMode == CaptureAlphaMode.OPAQUE) {
            for (int index = 0; index < pixels.length; index++) pixels[index] |= 0xFF000000;
        } else if (alphaMode == CaptureAlphaMode.PREMULTIPLIED) {
            for (int index = 0; index < pixels.length; index++) {
                int pixel = pixels[index];
                int alpha = pixel >>> 24;
                if (alpha == 0) pixels[index] = 0;
                else if (alpha < 255) {
                    int red = Math.min(255, ((pixel >>> 16) & 255) * 255 / alpha);
                    int green = Math.min(255, ((pixel >>> 8) & 255) * 255 / alpha);
                    int blue = Math.min(255, (pixel & 255) * 255 / alpha);
                    pixels[index] = alpha << 24 | red << 16 | green << 8 | blue;
                }
            }
        }
    }
}
