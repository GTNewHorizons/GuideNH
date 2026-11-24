package com.hfstudio.guidenh.client.capture.api;

import java.util.List;
import java.util.Set;

public record CaptureFormatSnapshot(List<String> formats, Set<String> animationFormats)
    implements CaptureFormatCatalog {

    public static final CaptureFormatSnapshot STANDARD = new CaptureFormatSnapshot(
        List.of("png", "gif", "sprites", "frames"),
        Set.of("gif", "sprites", "frames"));

    public CaptureFormatSnapshot {
        formats = List.copyOf(formats);
        animationFormats = Set.copyOf(animationFormats);
        if (!formats.containsAll(animationFormats))
            throw new IllegalArgumentException("Animation formats must be listed in the catalog");
    }

    @Override
    public boolean animationFormat(String format) {
        return animationFormats.contains(format);
    }
}
