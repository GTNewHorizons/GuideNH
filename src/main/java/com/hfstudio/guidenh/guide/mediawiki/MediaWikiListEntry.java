package com.hfstudio.guidenh.guide.mediawiki;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePageIcon;

public record MediaWikiListEntry(ResourceLocation pageId, String title, @Nullable GuidePageIcon icon, String sortKey) {}
