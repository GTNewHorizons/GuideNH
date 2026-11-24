package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.gui.GuiScreen;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderState;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureFrozenLayers implements AutoCloseable {

    private CaptureFrame gui;
    private CaptureFrame worldPreview;
    private GuiScreen screen;
    private CaptureViewport viewport;
    private CaptureContentOptions content;
    private CaptureRenderState target;
    private long revision;

    public boolean matches(CaptureContext context, CaptureContentOptions options, long refreshRevision) {
        return gui != null && screen == context.screen()
            && viewport.equals(context.viewport())
            && revision == refreshRevision
            && content.sameGuiLayer(options);
    }

    // Take ownership of the GUI and its optional composed world preview.
    public void replace(CaptureFrame guiFrame, CaptureFrame worldFrame, CaptureContext context,
        CaptureContentOptions options, long refreshRevision, CaptureRenderState readTarget) {
        if (guiFrame.width() != context.viewport()
            .pixelWidth() || guiFrame.height()
                != context.viewport()
                    .pixelHeight()
            || worldFrame != null
                && (worldFrame.width() != guiFrame.width() || worldFrame.height() != guiFrame.height()))
            throw new IllegalArgumentException("The frozen layers must match the capture viewport");
        close();
        gui = guiFrame;
        worldPreview = worldFrame;
        screen = context.screen();
        viewport = context.viewport();
        content = options;
        revision = refreshRevision;
        target = readTarget;
    }

    public CaptureRenderState target() {
        return target;
    }

    public boolean hasWorld() {
        return worldPreview != null;
    }

    public void includeWorld(CaptureWorldSnapshot snapshot) {
        CaptureFrame composed = snapshot.compose(gui);
        if (worldPreview != null) worldPreview.close();
        worldPreview = composed;
    }

    public CaptureFrame compose(boolean includeWorld) {
        return (includeWorld && worldPreview != null ? worldPreview : gui).retain();
    }

    @Override
    public void close() {
        if (gui != null) gui.close();
        if (worldPreview != null) worldPreview.close();
        gui = null;
        worldPreview = null;
        screen = null;
        viewport = null;
        content = null;
        target = null;
    }
}
