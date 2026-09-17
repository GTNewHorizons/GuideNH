package com.hfstudio.guidenh.guide.mediawiki.template;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public record MediaWikiTemplateDefinition(MediaWikiTemplateName name, ResourceLocation pageId,
    @Nullable ResourceLocation guideId, String sourcePack, String language, MediaWikiTemplateParameters parameters) {}
