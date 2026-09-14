package com.hfstudio.guidenh.guide;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

public record GuidePageChange(@Nullable String language, ResourceLocation pageId, @Nullable ParsedGuidePage oldPage,
    @Nullable ParsedGuidePage newPage) {

    @Deprecated
    public GuidePageChange(ResourceLocation pageId, @Nullable ParsedGuidePage oldPage,
        @Nullable ParsedGuidePage newPage) {
        this(null, pageId, oldPage, newPage);
    }
}
