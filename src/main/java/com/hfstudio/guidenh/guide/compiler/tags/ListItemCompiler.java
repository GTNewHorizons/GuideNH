package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytTaskListItem;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownListSemantics;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;

public class ListItemCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("li");
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytListItem listItem;
        var taskMarker = MarkdownListSemantics.extractTaskMarker(el.children());
        if (taskMarker != null) {
            LytTaskListItem taskItem = new LytTaskListItem();
            taskItem.setChecked(taskMarker.checked());

            // Strip the task-marker prefix from text nodes TEMPORARILY so the
            // compiled output omits "[x] "/"[ ] ".
            // Save original values and restore after compileBlockContext so the
            // AST remains immutable for re-compilation (CompileWorker may have
            // pre-compiled the same ParsedGuidePage on the guidenh-compile thread,
            // then RenderPageService compiles it again — mutation would lose the
            // marker on the second pass).
            MdxJsxFlowElement p = MarkdownListSemantics.findFirstP(el.children());
            List<String> savedPrefixTexts = p != null
                ? MarkdownListSemantics.stripPrefixInPlace(p, taskMarker.prefixLen())
                : List.of();

            listItem = taskItem;
            try {
                compiler.compileBlockContext(el.children(), listItem);
            } finally {
                // Restore original text values so AST is reusable
                if (p != null) {
                    MarkdownListSemantics.restoreTextValues(p, savedPrefixTexts);
                }
            }
        } else {
            listItem = new LytListItem();
            compiler.compileBlockContext(el.children(), listItem);
        }

        // Normalize first child margins
        var children = listItem.getChildren();
        if (!children.isEmpty()) {
            var firstChild = children.getFirst();
            if (firstChild instanceof LytBlock) {
                ((LytBlock) firstChild).setMarginTop(0);
                ((LytBlock) firstChild).setMarginBottom(0);
            }
        }
        parent.append(listItem);
    }
}
