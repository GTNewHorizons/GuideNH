package com.hfstudio.guidenh.client.capture.api;

public record CaptureTarget(String id, String parentId, String kind, String className, String label, CaptureRect bounds,
    CaptureRect visibleBounds, int zOrder, int priority, long ownerIdentity, String contentLayer,
    boolean playerInventory, String providerId) {

    public CaptureTarget(String id, String parentId, String kind, String className, String label, CaptureRect bounds,
        CaptureRect visibleBounds, int zOrder, int priority, long ownerIdentity, String contentLayer,
        boolean playerInventory) {
        this(
            id,
            parentId,
            kind,
            className,
            label,
            bounds,
            visibleBounds,
            zOrder,
            priority,
            ownerIdentity,
            contentLayer,
            playerInventory,
            "");
    }

    public CaptureTarget {
        providerId = providerId == null ? "" : providerId;
    }

    public CaptureTarget(String id, String parentId, String kind, String className, String label, CaptureRect bounds,
        CaptureRect visibleBounds, int zOrder, int priority, long ownerIdentity) {
        this(
            id,
            parentId,
            kind,
            className,
            label,
            bounds,
            visibleBounds,
            zOrder,
            priority,
            ownerIdentity,
            GUI_LAYER,
            false);
    }

    public CaptureTarget(String id, String parentId, String kind, String className, String label, CaptureRect bounds,
        CaptureRect visibleBounds, int zOrder, int priority) {
        this(id, parentId, kind, className, label, bounds, visibleBounds, zOrder, priority, 0);
    }

    public String trackingId() {
        return ownerIdentity == 0 ? "" : id + "@" + ownerIdentity;
    }

    public CaptureTarget withContent(String layer, boolean inventory) {
        return new CaptureTarget(
            id,
            parentId,
            kind,
            className,
            label,
            bounds,
            visibleBounds,
            zOrder,
            priority,
            ownerIdentity,
            layer,
            inventory,
            providerId);
    }

    public CaptureTarget withProvider(String provider) {
        return new CaptureTarget(
            id,
            parentId,
            kind,
            className,
            label,
            bounds,
            visibleBounds,
            zOrder,
            priority,
            ownerIdentity,
            contentLayer,
            playerInventory,
            provider);
    }

    public static final String SLOT = "slot";
    public static final String COMPONENT = "component";
    public static final String WINDOW = "window";
    public static final String SCREEN = "screen";
    public static final String TOOLTIP = "tooltip";
    public static final String GUI_LAYER = "gui";
    public static final String HUD_LAYER = "hud";
    public static final String NEI_LAYER = "nei";
    public static final String TOOLTIP_LAYER = "tooltip";
}
