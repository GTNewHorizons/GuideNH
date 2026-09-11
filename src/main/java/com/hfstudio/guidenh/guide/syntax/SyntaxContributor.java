package com.hfstudio.guidenh.guide.syntax;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

/**
 * Declares guide syntax to the editor.
 *
 * <p>
 * Register one per mod (or per subsystem) and push everything the editor should know into the
 * {@link SyntaxSink}. Third-party tags do not even need a contributor: a registered
 * {@code TagCompiler} already publishes its tag names, so its tags complete as soon as it exists.
 * A contributor adds the extra facts a compiler does not express - attribute names and value kinds,
 * container shape, child tags - plus markdown snippets, fence names and frontmatter keys.
 *
 * <p>
 * Register through {@code GuideBuilder.extension(SyntaxContributor.EXTENSION_POINT, contributor)} for
 * one guide, or {@code GuideNhIntegrationRegistry.registerSyntaxContributor(contributor)} to apply to
 * every guide.
 */
public interface SyntaxContributor extends Extension {

    ExtensionPoint<SyntaxContributor> EXTENSION_POINT = new ExtensionPoint<>(SyntaxContributor.class);

    /** Short identifier used in diagnostics, for example the owning mod id. */
    String namespace();

    void contribute(SyntaxSink sink);
}
