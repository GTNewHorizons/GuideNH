package com.hfstudio.guidenh.guide.mediawiki;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;

public record MediaWikiCategoryMember(String categoryName, @Nullable String sortKey, PageAnchor pageAnchor) {}
