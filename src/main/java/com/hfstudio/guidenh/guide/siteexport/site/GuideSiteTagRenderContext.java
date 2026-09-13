package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/** What a {@link GuideSiteTagRenderer} needs to render one element: the page it belongs to and the shared export. */
public record GuideSiteTagRenderContext(String defaultNamespace, @Nullable ResourceLocation currentPageId,
    GuideSiteTemplateRegistry templates, GuideSiteHtmlCompiler.SceneResolver sceneResolver,
    GuideSiteHtmlCompiler compiler) {}
