package com.hfstudio.guidenh.guide.syntax;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

/**
 * Declares guide syntax to the editor: attributes and value kinds, container shape, child tags, markdown.
 */
public interface SyntaxContributor extends Extension {

    ExtensionPoint<SyntaxContributor> EXTENSION_POINT = new ExtensionPoint<>(SyntaxContributor.class);

    String namespace();

    void contribute(SyntaxSink sink);
}
