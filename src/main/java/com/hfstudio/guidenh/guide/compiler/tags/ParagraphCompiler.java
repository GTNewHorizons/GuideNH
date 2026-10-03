package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LatexRenderOptions;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class ParagraphCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("p");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        // Sole $$...$$ block-level display formula detection.
        // A paragraph whose single text child is exactly $$formula$$ produces a
        // centered LytLatexDisplayBlock instead of an inline LytLatexBlock.
        if (el.children()
            .size() == 1) {
            Object sole = el.children()
                .getFirst();
            if (sole instanceof MdAstText soleText) {
                String formula = MarkdownLatexShorthand.extractSoleDisplayFormula(soleText.value);
                if (formula != null) {
                    var displayBlock = new LytLatexDisplayBlock(
                        formula,
                        LatexRenderOptions.builder()
                            .build());
                    displayBlock.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
                    displayBlock.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
                    parent.append(PageCompiler.wrapFloatAwareIfNeeded(displayBlock));
                    return;
                }
            }
        }

        // Default paragraph compilation (inline flow content).
        LytParagraph paragraph = new LytParagraph();
        compiler.compileFlowContext(el.children(), paragraph);
        paragraph.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        paragraph.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        parent.append(paragraph);
    }
}
