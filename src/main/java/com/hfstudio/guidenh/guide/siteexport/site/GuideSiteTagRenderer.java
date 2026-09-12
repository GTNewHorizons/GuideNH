package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Renders one kind of MDX tag into the exported site.
 *
 * <p>
 * The exported site knows the tags that ship with this mod. A mod whose tags are compiled by its own
 * {@code TagCompiler} registers a renderer here so its tags are exported too, instead of disappearing from
 * the site while they work in the book.
 *
 * <p>
 * Registered renderers are asked before the built-in ones and the first answer wins, so a renderer only
 * has to handle the tags it declares. A renderer that fails is reported and skipped.
 *
 * <p>
 * Register through {@code GuideBuilder.extension(GuideSiteTagRenderer.EXTENSION_POINT, renderer)} for one
 * guide, or {@code GuideNhIntegrationRegistry.registerSiteTagRenderer(renderer)} for every guide.
 */
public interface GuideSiteTagRenderer extends Extension {

    ExtensionPoint<GuideSiteTagRenderer> EXTENSION_POINT = new ExtensionPoint<>(GuideSiteTagRenderer.class);

    /** Tag names this renderer answers. */
    Set<String> getTagNames();

    /**
     * The HTML for one element.
     *
     * @return the rendered markup, or null to let another renderer answer
     */
    @Nullable
    String render(GuideSiteTagRenderContext context, MdxJsxElementFields element);
}
