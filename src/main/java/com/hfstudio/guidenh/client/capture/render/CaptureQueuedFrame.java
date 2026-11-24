package com.hfstudio.guidenh.client.capture.render;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrameIdentity;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CapturePhaseSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.api.CaptureReadbackMetadata;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public record CaptureQueuedFrame(CaptureReadback readback, CaptureContext context, CaptureFrameIdentity identity,
    CaptureGeometry geometry, long revision, CaptureWorldSnapshot world, CaptureRect cursorBounds,
    boolean previewRequested, long submittedAt, CaptureRecordedSnapshot snapshot) implements AutoCloseable {

    public CaptureQueuedFrame withPhases(List<CapturePhaseSnapshot> phases) {
        CaptureReadbackMetadata metadata = identity.readback();
        CaptureFrameIdentity updated = new CaptureFrameIdentity(
            identity.backend(),
            identity.requestedBackend(),
            identity.content(),
            new CaptureReadbackMetadata(context.frameId(), metadata.capturedPhase(), metadata.target(), phases));
        return new CaptureQueuedFrame(
            readback,
            context,
            updated,
            geometry,
            revision,
            world,
            cursorBounds,
            previewRequested,
            submittedAt,
            snapshot);
    }

    @Override
    public void close() {
        try {
            readback.close();
        } finally {
            if (world != null) world.close();
            if (snapshot != null) snapshot.close();
        }
    }
}
