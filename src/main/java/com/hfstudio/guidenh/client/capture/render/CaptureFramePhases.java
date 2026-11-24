package com.hfstudio.guidenh.client.capture.render;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CapturePhaseSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderPhase;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderState;

public class CaptureFramePhases {

    private CaptureContext context;
    private final List<CapturePhaseSnapshot> snapshots = new ArrayList<>(4);
    private boolean presentationPrepared;
    private boolean presented;

    public void begin(CaptureContext current) {
        context = current;
        snapshots.clear();
        presentationPrepared = false;
        presented = false;
    }

    public boolean active() {
        return context != null && !presented;
    }

    public CaptureRenderPhase phase() {
        return snapshots.isEmpty() ? null
            : snapshots.get(snapshots.size() - 1)
                .phase();
    }

    public CaptureRenderState renderState() {
        return context == null ? null : context.renderState();
    }

    public CaptureContext reach(CaptureRenderPhase phase, CaptureRenderState state, long timestamp) {
        if (!active() || phase() == phase) return null;
        if (phase.ordinal() != snapshots.size())
            throw new IllegalStateException("Unexpected capture phase: " + phase + " after " + phase());
        snapshots.add(new CapturePhaseSnapshot(context.frameId(), phase, timestamp, state));
        context = context.atPhase(phase, state);
        return context;
    }

    public void preparePresentation() {
        presentationPrepared = active() && phase() == CaptureRenderPhase.GUI_END;
    }

    public boolean readyToPresent() {
        return active() && presentationPrepared;
    }

    public void presented() {
        presented = true;
    }

    public List<CapturePhaseSnapshot> snapshots() {
        return List.copyOf(snapshots);
    }

    public void close() {
        context = null;
        snapshots.clear();
        presentationPrepared = false;
        presented = false;
    }
}
