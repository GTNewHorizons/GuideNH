package com.hfstudio.guidenh.guide.mediawiki.template;

import net.minecraft.util.ResourceLocation;

public record MediaWikiTemplateDefinition(MediaWikiTemplateName name, ResourceLocation pageId, String sourcePack,
    String language, MediaWikiTemplateParameters parameters) {}
