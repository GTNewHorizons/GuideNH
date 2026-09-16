package com.hfstudio.guidenh.guide.compiler;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public record FrontmatterPageMeta(List<String> authors, @Nullable String date, @Nullable String updated, float zoom) {

    public boolean isEmpty() {
        return authors.isEmpty() && date == null && updated == null;
    }
}
