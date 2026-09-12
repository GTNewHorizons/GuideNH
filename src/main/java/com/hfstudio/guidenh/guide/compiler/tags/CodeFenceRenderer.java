package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

/**
 * Renders the body of a code fence whose name you own; declare the name through
 * {@code SyntaxSink.fenceLanguages(...)} too, so the editor offers it. Register through
 * {@code GuideNhIntegrationRegistry.registerCodeFenceRenderer} or {@code GuideBuilder.extension}.
 */
public interface CodeFenceRenderer extends Extension {

    ExtensionPoint<CodeFenceRenderer> EXTENSION_POINT = new ExtensionPoint<>(CodeFenceRenderer.class);

    /** Fence names this renderer answers, as written after the three backticks. */
    Set<String> getFenceNames();

    /**
     * The block a fence body becomes.
     *
     * @return the block to append, or null to let another renderer, or the built-in handling, answer
     */
    @Nullable
    LytBlock renderFence(PageCompiler compiler, String fenceName, String codeText, @Nullable String meta);

    /**
     * The HTML a fence body becomes on the exported site, where a fence this renderer owns would otherwise
     * be shown as a plain code block. The markup is inserted as it is, so escape any text in it.
     */
    @Nullable
    default String renderSiteFence(String fenceName, String codeText, @Nullable String meta) {
        return null;
    }
}
