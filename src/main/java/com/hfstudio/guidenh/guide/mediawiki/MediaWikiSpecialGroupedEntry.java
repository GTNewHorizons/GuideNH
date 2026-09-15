package com.hfstudio.guidenh.guide.mediawiki;

import java.util.List;

public record MediaWikiSpecialGroupedEntry(String title, String summary, String searchBlob,
    List<MediaWikiSpecialListEntry> children) {}
