package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.mixins.early.minecraft.AccessorCaptureFontStrings;

public class CaptureTextReplayCommand implements CaptureReplayCommand {

    private final CaptureRecordedGlState state;
    private final FontRenderer font;
    private final String text;
    private final int x;
    private final int y;
    private final int color;
    private final boolean shadow;
    private final boolean unicode;
    private final boolean bidi;

    public CaptureTextReplayCommand(CaptureTextureSnapshotStore textures, FontRenderer font, String text, int x, int y,
        int color, boolean shadow) {
        state = new CaptureRecordedGlState(textures, true);
        this.font = font;
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.shadow = shadow;
        unicode = font.getUnicodeFlag();
        bidi = font.getBidiFlag();
    }

    @Override
    public void draw(CaptureReplayTransform transform) {
        boolean previousUnicode = font.getUnicodeFlag();
        boolean previousBidi = font.getBidiFlag();
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            state.apply(transform);
            font.setUnicodeFlag(unicode);
            font.setBidiFlag(bidi);
            AccessorCaptureFontStrings access = (AccessorCaptureFontStrings) font;
            access.guidenh$resetCapturedStringStyles();
            access.guidenh$renderCapturedString(text, x, y, color, shadow);
        } finally {
            font.setUnicodeFlag(previousUnicode);
            font.setBidiFlag(previousBidi);
        }
    }
}
