package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

public record GuideSitePageVariant(ResourceLocation guideId, ResourceLocation pageId, String language,
    String sourceLanguage, boolean fallbackUsed, ParsedGuidePage parsedPage) {}
