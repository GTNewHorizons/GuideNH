package com.hfstudio.guidenh.client.capture.render;

import org.lwjgl.opengl.GL11;

public class CaptureAlphaBlend {

    private CaptureAlphaBlend() {}

    public static boolean sourceOver(int source, int destination) {
        return (source == GL11.GL_SRC_ALPHA || source == GL11.GL_ONE) && destination == GL11.GL_ONE_MINUS_SRC_ALPHA;
    }
}
