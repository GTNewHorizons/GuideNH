package com.hfstudio.guidenh.guide.compiler;

import java.util.Set;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

/** Expands a registered Markdown action scheme into an ordinary inline tag. */
public interface ActionLinkResolver extends Extension {

    ExtensionPoint<ActionLinkResolver> EXTENSION_POINT = new ExtensionPoint<>(ActionLinkResolver.class);

    Set<String> getSchemes();

    MdxJsxTextElement resolve(String label, String destination, String defaultNamespace);
}
