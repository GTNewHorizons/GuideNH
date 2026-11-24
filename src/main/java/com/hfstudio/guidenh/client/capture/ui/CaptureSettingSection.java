package com.hfstudio.guidenh.client.capture.ui;

public enum CaptureSettingSection {

    OUTPUT("output"),
    CONTENT("content"),
    SNAPPING("snapping"),
    INTERACTION("interaction"),
    RECORDING("recording"),
    SCHEDULE("schedule"),
    BATCH("batch");

    private final String id;

    private CaptureSettingSection(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static CaptureSettingSection forKey(String key) {
        if (key.startsWith("batch_") || key.startsWith("server_")) return BATCH;
        if (key.startsWith("snap_") || key.equals("snapping") || key.equals("class_name")) return SNAPPING;
        if (key.startsWith("selection_") || key.equals("handle_size") || key.equals("redraw_all")) return INTERACTION;
        if (key.startsWith("gif_") || key.startsWith("sprite_")) return RECORDING;
        if (key.startsWith("schedule_")) return SCHEDULE;
        return switch (key) {
            case "world", "gui", "player_inventory", "hud", "cursor", "backdrop", "tooltips", "debug", "nei" -> CONTENT;
            case "capture_area", "freeze", "mask_opacity", "invert_transparency_grid" -> INTERACTION;
            case "fps", "record_seconds", "frame_limit", "loop_count", "alpha_threshold", "queue_frames", "strict_queue", "target_change" -> RECORDING;
            case "delay", "interval", "repeat", "scheduled_time", "unavailable" -> SCHEDULE;
            case "stable_frames", "timeout", "target_limit" -> BATCH;
            default -> OUTPUT;
        };
    }
}
