package com.hfstudio.guidenh.client.capture.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;

public class CaptureRecordedSnapshot implements CaptureRenderSnapshot {

    private final CaptureContext context;
    private final CaptureContentOptions content;
    private final CaptureTextureSnapshotStore textures;
    private final List<CaptureReplayCommand> commands = new ArrayList<>();
    private final long budget;
    private final CaptureMemoryBudget memory;
    private final Thread owner = Thread.currentThread();
    private long commandBytes;
    private boolean sealed;
    private int references = 1;

    public CaptureRecordedSnapshot(CaptureContext context, CaptureContentOptions content, long budget) {
        this(context, content, budget, new CaptureMemoryBudget(budget));
    }

    public CaptureRecordedSnapshot(CaptureContext context, CaptureContentOptions content, long budget,
        CaptureMemoryBudget memory) {
        if (budget < 0) throw new IllegalArgumentException("The draw snapshot budget cannot be negative");
        this.context = context;
        this.content = content;
        this.budget = budget;
        this.memory = Objects.requireNonNull(memory);
        textures = new CaptureTextureSnapshotStore(() -> budget - commandBytes, memory);
    }

    public CaptureTextureSnapshotStore textures() {
        return textures;
    }

    public void reserve(long bytes) {
        checkRecording();
        if (bytes < 0) throw new IllegalArgumentException("Draw snapshot reservations cannot be negative");
        if (bytes > budget - commandBytes - textures.bytes())
            throw new IllegalArgumentException("The draw snapshot exceeds the memory budget");
        memory.reserve(bytes);
        commandBytes += bytes;
    }

    public void add(CaptureReplayCommand command) {
        checkRecording();
        if (textures.bytes() + commandBytes > budget)
            throw new IllegalArgumentException("The draw snapshot exceeds the memory budget");
        commands.add(Objects.requireNonNull(command));
    }

    public CaptureRecordedSnapshot seal() {
        checkRecording();
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
        checkThread();
        if (references <= 0 || !sealed) throw new IllegalStateException("The draw snapshot is unavailable");
        references = Math.incrementExact(references);
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
        checkThread();
        if (references <= 0 || !sealed) throw new IllegalStateException("The draw snapshot is unavailable");
        CaptureReplayTransform transform = new CaptureReplayTransform(context.viewport());
        for (CaptureReplayCommand command : commands) command.draw(transform);
    }

    @Override
    public void close() {
        checkThread();
        if (references <= 0) throw new IllegalStateException("The draw snapshot was released twice");
        if (--references == 0) {
            commands.clear();
            memory.release(commandBytes);
            commandBytes = 0;
            textures.close();
        }
    }

    private void checkRecording() {
        checkThread();
        if (sealed || references <= 0) throw new IllegalStateException("The draw snapshot is no longer recording");
    }

    private void checkThread() {
        if (Thread.currentThread() != owner)
            throw new IllegalStateException("Draw snapshots belong to their creating render thread");
    }
}
