package com.hfstudio.guidenh.guide.mediawiki;

import org.jetbrains.annotations.Nullable;

public record MediaWikiCategoryReference(String categoryName, @Nullable String sortKey) {}
