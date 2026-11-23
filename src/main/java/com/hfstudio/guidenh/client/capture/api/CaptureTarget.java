package com.hfstudio.guidenh.client.capture.api;

public record CaptureTarget(String id, String parentId, String kind, String className, String label, CaptureRect bounds,
    CaptureRect visibleBounds, int zOrder, int priority, long ownerIdentity) {

    public CaptureTarget(String id, String parentId, String kind, String className, String label, CaptureRect bounds,
        CaptureRect visibleBounds, int zOrder, int priority) {
        this(id, parentId, kind, className, label, bounds, visibleBounds, zOrder, priority, 0);
    }

    public String trackingId() {
        return ownerIdentity == 0 ? "" : id + "@" + ownerIdentity;
    }

    public static final String SLOT = "slot";
    public static final String COMPONENT = "component";
    public static final String WINDOW = "window";
    public static final String SCREEN = "screen";
}
