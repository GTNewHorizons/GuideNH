package com.hfstudio.guidenh.client.capture.render;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;

public class CaptureRecordedSnapshot implements CaptureRenderSnapshot {

    private final CaptureContext context;
    private final CaptureContentOptions content;
    private final CaptureTextureSnapshotStore textures;
    private final List<CaptureReplayCommand> commands = new ArrayList<>();
    private final long budget;
    private long commandBytes;
    private boolean sealed;
    private int references = 1;

    public CaptureRecordedSnapshot(CaptureContext context, CaptureContentOptions content, long budget) {
        this.context = context;
        this.content = content;
        this.budget = budget;
        textures = new CaptureTextureSnapshotStore(budget);
    }

    public CaptureTextureSnapshotStore textures() {
        return textures;
    }

    public void reserve(long bytes) {
        if (sealed || references <= 0) throw new IllegalStateException("The draw snapshot is no longer recording");
        if (bytes > budget - commandBytes - textures.bytes())
            throw new IllegalArgumentException("The draw snapshot exceeds the memory budget");
        commandBytes += bytes;
    }

    public void add(CaptureReplayCommand command) {
        if (textures.bytes() + commandBytes > budget)
            throw new IllegalArgumentException("The draw snapshot exceeds the memory budget");
        commands.add(command);
    }

    public CaptureRecordedSnapshot seal() {
        sealed = true;
        return this;
    }

    public boolean matches(CaptureContext current, CaptureContentOptions options) {
        return references > 0 && sealed
            && context.screen() == current.screen()
            && context.viewport()
                .equals(current.viewport())
            && content.sameGuiLayer(options);
    }

    public CaptureRecordedSnapshot retain() {
        if (references <= 0 || !sealed) throw new IllegalStateException("The draw snapshot is unavailable");
        references++;
        return this;
    }

    @Override
    public boolean includesHud() {
        return content.includeHud();
    }

    @Override
    public boolean filtersContent() {
        return true;
    }

    @Override
    public void draw() {
        if (references <= 0 || !sealed) throw new IllegalStateException("The draw snapshot is unavailable");
        CaptureReplayTransform transform = new CaptureReplayTransform(context.viewport());
        for (CaptureReplayCommand command : commands) command.draw(transform);
    }

    @Override
    public void close() {
        if (references <= 0) throw new IllegalStateException("The draw snapshot was released twice");
        if (--references == 0) {
            commands.clear();
            textures.close();
        }
    }
}
