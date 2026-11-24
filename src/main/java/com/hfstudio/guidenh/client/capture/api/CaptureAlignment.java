package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public enum CaptureAlignment {

    TOP_LEFT("top_left", 0, 0),
    TOP_CENTER("top_center", 1, 0),
    TOP_RIGHT("top_right", 2, 0),
    CENTER_LEFT("center_left", 0, 1),
    CENTER("center", 1, 1),
    CENTER_RIGHT("center_right", 2, 1),
    BOTTOM_LEFT("bottom_left", 0, 2),
    BOTTOM_CENTER("bottom_center", 1, 2),
    BOTTOM_RIGHT("bottom_right", 2, 2);

    private static final List<CaptureAlignment> ALIGNMENTS = List.of(values());
    private static final List<String> IDS = ALIGNMENTS.stream()
        .map(CaptureAlignment::id)
        .toList();
    private final String id;
    private final int horizontalAnchor;
    private final int verticalAnchor;

    private CaptureAlignment(String id, int horizontalAnchor, int verticalAnchor) {
        this.id = id;
        this.horizontalAnchor = horizontalAnchor;
        this.verticalAnchor = verticalAnchor;
    }

    public String id() {
        return id;
    }

    public static List<String> ids() {
        return IDS;
    }

    public static CaptureAlignment byId(String id) {
        for (CaptureAlignment alignment : ALIGNMENTS) {
            if (alignment.id.equals(id)) return alignment;
        }
        throw new IllegalArgumentException("Unknown capture alignment: " + id);
    }

    public int offsetX(int space) {
        return offset(space, horizontalAnchor);
    }

    public int offsetY(int space) {
        return offset(space, verticalAnchor);
    }

    private static int offset(int space, int anchor) {
        if (space < 0) throw new IllegalArgumentException("The capture region exceeds its layout cell");
        return anchor == 0 ? 0 : anchor == 1 ? space / 2 : space;
    }
}
