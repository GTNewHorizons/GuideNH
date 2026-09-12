package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * What a {@link GuideSiteTagRenderer} needs to render one element: the page it belongs to and the shared
 * export services the built-in renderers use as well.
 *
 * @param defaultNamespace namespace of the guide being exported
 * @param currentPageId    page the element was found on, or null outside a page
 * @param templates        template lookup of the export
 * @param sceneResolver    hands out the scenes collected for this export
 * @param compiler         the compiler running the export, for resolving nested content
 */
public record GuideSiteTagRenderContext(String defaultNamespace, @Nullable ResourceLocation currentPageId,
    GuideSiteTemplateRegistry templates, GuideSiteHtmlCompiler.SceneResolver sceneResolver,
    GuideSiteHtmlCompiler compiler) {}
