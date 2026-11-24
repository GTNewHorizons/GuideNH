package com.hfstudio.guidenh.client.capture.render;

import org.lwjgl.opengl.GL11;

public class CaptureAlphaBlend {

    private CaptureAlphaBlend() {}

    public static boolean sourceOver(int source, int destination) {
        return (source == GL11.GL_SRC_ALPHA || source == GL11.GL_ONE) && destination == GL11.GL_ONE_MINUS_SRC_ALPHA;
    }

    public static int sourceAlpha(int source, int destination, int sourceAlpha, int destinationAlpha) {
        if (sourceAlpha == GL11.GL_ZERO) return sourceAlpha;
        return sourceOver(source, destination) ? GL11.GL_ONE : sourceAlpha;
    }

    public static int destinationAlpha(int source, int destination, int sourceAlpha, int destinationAlpha) {
        // Native item passes clear alpha without changing visible RGB; preserve existing layer coverage.
        if (sourceAlpha == GL11.GL_ZERO && destinationAlpha == GL11.GL_ZERO) return GL11.GL_ONE;
        if (sourceAlpha == GL11.GL_ZERO) return destinationAlpha;
        return sourceOver(source, destination) ? GL11.GL_ONE_MINUS_SRC_ALPHA : destinationAlpha;
    }
}
