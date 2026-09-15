package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/** Renders one kind of MDX tag into the exported site, so a mod whose own {@code TagCompiler} compiles its tags. */
public interface GuideSiteTagRenderer extends Extension {

    ExtensionPoint<GuideSiteTagRenderer> EXTENSION_POINT = new ExtensionPoint<>(GuideSiteTagRenderer.class);

    Set<String> getTagNames();

    @Nullable
    String render(GuideSiteTagRenderContext context, MdxJsxElementFields element);
}
