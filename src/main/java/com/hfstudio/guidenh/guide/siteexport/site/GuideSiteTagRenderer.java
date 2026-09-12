package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Renders one kind of MDX tag into the exported site, so a mod whose own {@code TagCompiler} compiles its
 * tags exports them too. Registered renderers are asked before the built-in ones; register through
 * {@code GuideNhIntegrationRegistry.registerSiteTagRenderer} or {@code GuideBuilder.extension}.
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
