package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.editor.SceneEditorIcon;

public class CaptureIcons {

    public static final ResourceLocation ATLAS = new ResourceLocation("guidenh", "textures/capture/icons.png");
    public static final SceneEditorIcon SAVE = icon(0);
    public static final SceneEditorIcon CAPTURE_ALL = icon(1);
    public static final SceneEditorIcon CROP = icon(2);
    public static final SceneEditorIcon ADD = icon(3);
    public static final SceneEditorIcon UNDO = icon(4);
    public static final SceneEditorIcon REDO = icon(5);
    public static final SceneEditorIcon SETTINGS = icon(6);
    public static final SceneEditorIcon WORLD = icon(7);
    public static final SceneEditorIcon SNAP = icon(8);
    public static final SceneEditorIcon REFRESH = icon(9);
    public static final SceneEditorIcon RECORD = icon(10);
    public static final SceneEditorIcon STOP = icon(11);
    public static final SceneEditorIcon PLAY = icon(12);
    public static final SceneEditorIcon PAUSE = icon(13);
    public static final SceneEditorIcon TIMER = icon(14);
    public static final SceneEditorIcon CLOSE = icon(15);
    public static final SceneEditorIcon DELETE = icon(16);
    public static final SceneEditorIcon FOLDER = icon(17);
    public static final SceneEditorIcon LOCK = icon(18);
    public static final SceneEditorIcon LINK = icon(19);
    public static final SceneEditorIcon UP = icon(20);
    public static final SceneEditorIcon DOWN = icon(21);
    public static final SceneEditorIcon CHECK = icon(22);
    public static final SceneEditorIcon COPY = icon(23);
    public static final SceneEditorIcon ERASE = icon(24);

    private CaptureIcons() {}

    private static SceneEditorIcon icon(int index) {
        return new SceneEditorIcon(ATLAS, 256, 256, index % 16 * 16, index / 16 * 16, 16, 16);
    }
}
