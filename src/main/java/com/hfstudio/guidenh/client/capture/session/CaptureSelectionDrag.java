package com.hfstudio.guidenh.client.capture.session;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureResizeEdges;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public record CaptureSelectionDrag(long regionId, String kind, double originX, double originY,
    CaptureRect initialBounds, CaptureResizeEdges edges, CaptureTarget clickTarget) {

    public static final String CREATE = "create";
    public static final String MOVE = "move";
    public static final String RESIZE = "resize";
}
