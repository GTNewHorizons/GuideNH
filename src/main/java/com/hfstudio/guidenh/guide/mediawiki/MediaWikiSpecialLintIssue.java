package com.hfstudio.guidenh.guide.mediawiki;

import org.jetbrains.annotations.Nullable;

public record MediaWikiSpecialLintIssue(String message, @Nullable Integer lineNumber) {}
