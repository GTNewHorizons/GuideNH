package com.hfstudio.guidenh.client.capture.export;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;

public class CaptureGifPalette {

    private static final int[] BAYER = { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };
    private final IndexColorModel colors;

    public CaptureGifPalette() {
        byte[] red = new byte[256];
        byte[] green = new byte[256];
        byte[] blue = new byte[256];
        for (int index = 0; index < 216; index++) {
            red[index + 1] = (byte) (index / 36 * 51);
            green[index + 1] = (byte) (index / 6 % 6 * 51);
            blue[index + 1] = (byte) (index % 6 * 51);
        }
        for (int index = 217; index < 256; index++) {
            byte shade = (byte) Math.round((index - 217) * 255d / 38);
            red[index] = green[index] = blue[index] = shade;
        }
        colors = new IndexColorModel(8, 256, red, green, blue, 0);
    }

    public BufferedImage convert(CaptureRowComposer rows, CaptureRecordingOptions options, int background) {
        BufferedImage image = new BufferedImage(
            rows.size()
                .width(),
            rows.size()
                .height(),
            BufferedImage.TYPE_BYTE_INDEXED,
            colors);
        byte[] pixels = ((DataBufferByte) image.getRaster()
            .getDataBuffer()).getData();
        int[] row = new int[rows.size()
            .width()];
        for (int y = 0; y < rows.size()
            .height(); y++) {
            rows.row(y, row);
            for (int x = 0; x < row.length; x++) {
                int pixel = row[x];
                if (!options.matteTransparency() && pixel >>> 24 < options.alphaThreshold()) {
                    pixels[y * row.length + x] = 0;
                    continue;
                }
                if (options.matteTransparency()) pixel = CaptureRowComposer.composite(pixel, background | 0xFF000000);
                int noise = options.dither() ? (BAYER[(y & 3) * 4 + (x & 3)] * 3 - 22) : 0;
                int r = Math.clamp(((pixel >>> 16) & 255) + noise, 0, 255);
                int g = Math.clamp(((pixel >>> 8) & 255) + noise, 0, 255);
                int b = Math.clamp((pixel & 255) + noise, 0, 255);
                int ri = (r + 25) / 51;
                int gi = (g + 25) / 51;
                int bi = (b + 25) / 51;
                int cubeError = square(r - ri * 51) + square(g - gi * 51) + square(b - bi * 51);
                int greyIndex = Math.round((r + g + b) * 38f / (3 * 255));
                int grey = (int) Math.round(greyIndex * 255d / 38);
                int greyError = square(r - grey) + square(g - grey) + square(b - grey);
                pixels[y * row.length
                    + x] = (byte) (greyError < cubeError ? 217 + greyIndex : 1 + ri * 36 + gi * 6 + bi);
            }
        }
        return image;
    }

    private static int square(int value) {
        return value * value;
    }
}
