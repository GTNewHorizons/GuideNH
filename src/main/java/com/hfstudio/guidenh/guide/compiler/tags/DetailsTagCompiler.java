package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDetailsBlock;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

public class DetailsTagCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("details");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytDetailsBlock details = new LytDetailsBlock();
        details.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        details.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        details.setOpen(el.hasAttribute("open"));
        details.setFallbackSummaryText("Details");

        // Reuse the dedented body AST for source pages and the substituted AST for templates.
        // The inline tokenizer, rather than a string match, owns the summary's closing boundary.
        var children = compiler.reparseBlockTagChildren(el);
        compiler.withBlockTagChildrenSourceContext(el, () -> compileAstChildren(compiler, details, children));

        Integer width = readOptionalInt(el, "width");
        Integer height = readOptionalInt(el, "height");
        if (width != null) {
            details.setPreferredWidth(width);
        }
        if (height != null) {
            details.setPreferredContentHeight(height);
        }
        parent.append(details);
    }

    private void compileAstChildren(PageCompiler compiler, LytDetailsBlock details,
        List<? extends MdAstAnyContent> children) {
        var extracted = DetailsContentExtractor.extractChildren(children);
        if (extracted.summary() != null) {
            details.getSummaryBox()
                .clearContent();
            compiler.compileInlineFragment(
                DetailsContentExtractor.summaryChildren(extracted.summary()),
                details.getSummaryBox());
            if (details.getSummaryBox()
                .isEmpty()) {
                details.setFallbackSummaryText("Details");
            }
        }

        if (!extracted.body()
            .isEmpty()) {
            compiler.compileBlockContext(extracted.body(), details.getContentBox());
        }
    }

    private Integer readOptionalInt(MdxJsxElementFields el, String name) {
        String raw = el.getAttributeString(name, null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
