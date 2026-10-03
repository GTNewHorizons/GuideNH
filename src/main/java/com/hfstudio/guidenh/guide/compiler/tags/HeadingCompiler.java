package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class HeadingCompiler extends BlockTagCompiler {

    private static final Set<String> TAG_NAMES = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

    @Override
    public Set<String> getTagNames() {
        return TAG_NAMES;
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytHeading heading = new LytHeading();
        int depth = parseIntSafe(el.getAttributeString("depth", "1"), 1);
        heading.setDepth(Math.max(1, Math.min(depth, 6)));
        compiler.compileFlowContext(el.children(), heading);
        parent.append(heading);
        // Adjacent-heading margin collapse: taffy adds margins without CSS
        // collapsing, so two consecutive headings would sum the first's bottom
        // margin and the second's top margin into an oversized gap (H3 7 + H4 12
        // ≈ 19px hole). When the sibling directly before this heading is also a
        // heading, zero its bottom margin so the pair keeps only this heading's
        // top margin (see LytHeading#collapseBottomForAdjacent). Heading→body
        // spacing is untouched.
        LytNode holder = heading.getParent();
        if (holder != null) {
            var siblings = holder.getChildren();
            int size = siblings.size();
            if (size >= 2 && siblings.get(size - 2) instanceof LytHeading previous) {
                previous.collapseBottomForAdjacent();
            }
        }
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
