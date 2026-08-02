package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytAlertBox;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytQuoteBox;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.BlockquoteDirective;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class BlockquoteCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("blockquote");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        BlockquoteDirective directive = MarkdownRuntimeBlocks.parseBlockquoteDirective(el);
        if (directive != null && directive.alertType() != null) {
            LytAlertBox alertBox = new LytAlertBox();
            alertBox.setTitle(
                directive.alertType()
                    .displayText(),
                directive.alertType());
            alertBox.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
            alertBox.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
            compileDirectiveBody(compiler, directive, alertBox);
            normalizeBlockMargins(alertBox);
            parent.append(PageCompiler.wrapFloatAwareIfNeeded(alertBox));
            return;
        }

        if (directive != null && (directive.title() != null || directive.icon() != null)) {
            LytQuoteBox quoteBox = new LytQuoteBox();
            quoteBox.setQuoteStyle(
                directive.accentColor(),
                directive.title(),
                CalloutIconSupport.buildFlowIcon(compiler, directive.icon()));
            quoteBox.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
            quoteBox.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
            compileDirectiveBody(compiler, directive, quoteBox);
            normalizeBlockMargins(quoteBox);
            shiftFirstParagraphDown(quoteBox, 1);
            parent.append(PageCompiler.wrapFloatAwareIfNeeded(quoteBox));
            return;
        }

        // Plain blockquote
        LytVBox blockquote = new LytVBox();
        blockquote.setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        blockquote.setPadding(5);
        blockquote.setPaddingLeft(10);
        blockquote.setBorderLeft(new BorderStyle(SymbolicColor.TABLE_BORDER, 2));
        blockquote.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        blockquote.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        compiler.compileBlockContext(el.children(), blockquote);
        normalizeBlockMargins(blockquote);
        shiftFirstParagraphDown(blockquote, 1);
        parent.append(PageCompiler.wrapFloatAwareIfNeeded(blockquote));
    }

    private void compileDirectiveBody(PageCompiler compiler, BlockquoteDirective directive, LytBlockContainer parent) {
        // When there's a remainingText override and the first paragraph is still present
        // at the head of the children list, replace its leading text.
        // Otherwise — just compile children normally.
        if (!directive.children()
            .isEmpty() && directive.firstParagraph() != null
            && directive.children()
                .getFirst() == directive.firstParagraph()
            && directive.remainingText() != null
            && !directive.remainingText()
                .isEmpty()) {
            // Strip directive prefix from the first paragraph's leading text
            stripLeadingText(directive.firstParagraph(), directive.remainingText());
            compiler.compileBlockContext(Collections.singletonList(directive.firstParagraph()), parent);
            for (int i = 1; i < directive.children()
                .size(); i++) {
                compiler.compileBlockContext(
                    Collections.singletonList(
                        directive.children()
                            .get(i)),
                    parent);
            }
        } else {
            compiler.compileBlockContext(directive.children(), parent);
        }
    }

    private void normalizeBlockMargins(LytNode box) {
        // The alert/quote box title row is an independent margin-less paragraph,
        // so the FIRST BODY paragraph still carries the block-paragraph top
        // margin (5) that would otherwise double the title-row gap (VBox gap 4 +
        // margin 5 ≈ the reported one-line blank band). The plain blockquote has
        // no title row and its children ARE the body paragraphs.
        if (box instanceof LytQuoteBox quoteBox) {
            // Body paragraphs live in a nested content container below the title
            // row; clear the first body paragraph's top margin so the title-row
            // spacing is carried by the container's gap alone.
            clearFirstParagraphTopMargin(quoteBox.getBodyContainer());
            return;
        }
        var children = box.getChildren();
        // LytAlertBox always carries its title row at index 0 (appended in its
        // constructor), so the first BODY paragraph is at index 1.
        int firstBody = box instanceof LytAlertBox ? 1 : 0;
        if (children.size() > firstBody && children.get(firstBody) instanceof LytParagraph first) {
            first.setMarginTop(0);
        }
        if (!children.isEmpty() && children.getLast() instanceof LytParagraph last) {
            last.setMarginBottom(0);
        }
    }

    private static void clearFirstParagraphTopMargin(LytNode container) {
        var children = container.getChildren();
        if (!children.isEmpty() && children.getFirst() instanceof LytParagraph first) {
            first.setMarginTop(0);
        }
    }

    private void shiftFirstParagraphDown(LytNode box, int pixels) {
        var boxChildren = box.getChildren();
        if (!boxChildren.isEmpty() && boxChildren.getFirst() instanceof LytParagraph first) {
            first.setPaddingTop(first.getPaddingTop() + pixels);
        }
    }

    private static void stripLeadingText(MdxJsxFlowElement paragraph, String replacementText) {
        for (Object child : paragraph.children()) {
            if (child instanceof MdAstText text && !text.value.trim()
                .isEmpty()) {
                text.setValue(replacementText);
                return;
            }
        }
    }
}
