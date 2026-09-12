package com.hfstudio.guidenh.guide.syntax;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

/**
 * Declares guide syntax to the editor: attributes and value kinds, container shape, child tags, markdown
 * snippets, fence names and frontmatter keys - the facts a tag compiler cannot express. Register through
 * {@code GuideNhIntegrationRegistry.registerSyntaxContributor}, or per guide with {@code GuideBuilder}.
 */
public interface SyntaxContributor extends Extension {

    ExtensionPoint<SyntaxContributor> EXTENSION_POINT = new ExtensionPoint<>(SyntaxContributor.class);

    /** Short identifier used in diagnostics, for example the owning mod id. */
    String namespace();

    void contribute(SyntaxSink sink);
}
