package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

/**
 * Tag compiler for {@code <NodeContent>} — rich content blocks inside
 * {@code <Mermaid>} diagrams.
 *
 * <p>NodeContent is only meaningful as a child of {@code <Mermaid>}, where
 * the {@link MermaidCompiler} extracts and compiles it directly from the
 * element tree (see {@code compileNodeContentBlocks}). At the page level
 * this compiler acts as a no-op (with a debug log) to prevent the
 * "Unhandled MDX: NodeContent" error.
 */
public class NodeContentTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("NodeContent");
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        // NodeContent is only valid as a child of <Mermaid>.  The MermaidCompiler
        // handles extraction and compilation directly from el.children().
        // At page level: no-op to suppress "Unhandled MDX" error.
        String id = el.getAttributeString("id", null);
        GuideDebugLog.debug(
            "[GuideNH] [NodeContentTagCompiler] Ignored at page level (id={})",
            id != null ? id : "<null>");
    }

    @Override
    public void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        // Inline NodeContent is not valid; silently ignore.
        GuideDebugLog.debug("[GuideNH] [NodeContentTagCompiler] Ignored in flow context");
    }
}
