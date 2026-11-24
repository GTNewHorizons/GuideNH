package com.hfstudio.guidenh.client.capture.export;

import java.util.List;

public record CaptureContactSheetManifest(String group, List<Page> pages, List<Entry> entries) {

    public CaptureContactSheetManifest {
        pages = List.copyOf(pages);
        entries = List.copyOf(entries);
    }

    public record Page(String file, int width, int height) {}

    public record Entry(String targetId, String label, String source, String file, int x, int y, int width,
        int height) {}
}
