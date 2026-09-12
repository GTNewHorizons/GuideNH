package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

/**
 * Renders the body of a code fence whose name you own.
 *
 * <p>
 * A fence name that is only completed is a name the reader sees as a plain code block. Register a renderer
 * for the name to decide what its body means, and declare the same name through
 * {@code SyntaxSink.fenceLanguages(...)} so the editor offers it.
 *
 * <p>
 * Register through {@code GuideBuilder.extension(CodeFenceRenderer.EXTENSION_POINT, renderer)} for one
 * guide, or {@code GuideNhIntegrationRegistry.registerCodeFenceRenderer(renderer)} for every guide. A
 * renderer is asked before the built-in fence handling and the first answer wins.
 */
public interface CodeFenceRenderer extends Extension {

    ExtensionPoint<CodeFenceRenderer> EXTENSION_POINT = new ExtensionPoint<>(CodeFenceRenderer.class);

    /** Fence names this renderer answers, as written after the three backticks. */
    Set<String> getFenceNames();

    /**
     * The block a fence body becomes.
     *
     * @param fenceName the name written after the three backticks
     * @param codeText  the body of the fence
     * @param meta      the {@code meta} attribute of the element, or null
     * @return the block to append, or null to let another renderer, or the built-in handling, answer
     */
    @Nullable
    LytBlock renderFence(PageCompiler compiler, String fenceName, String codeText, @Nullable String meta);

    /**
     * The HTML a fence body becomes on the exported site.
     *
     * <p>
     * A fence a renderer owns in the book is shown on the site as a plain code block unless the renderer
     * answers here too, which would leave the reader with raw source where the book draws the rendered
     * form. The markup is inserted into the page as it is, so escape any text in it.
     *
     * @return the markup, or null to show the body as a code block
     */
    @Nullable
    default String renderSiteFence(String fenceName, String codeText, @Nullable String meta) {
        return null;
    }
}
