package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.DetailsContentExtractor.DetailsContent;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDetailsBlock;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class DetailsTagCompiler extends BlockTagCompiler {

    /** A leading summary tag and its content, which the parser may leave as literal text. */
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("<summary>(.*?)</summary>", Pattern.DOTALL);

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

        String childrenSource = compiler.getBlockTagChildrenSource(el);
        if (childrenSource != null) {
            DetailsContent extracted = DetailsContentExtractor.extract(childrenSource);
            if (extracted.summaryMarkdown() != null) {
                details.getSummaryBox()
                    .clearContent();
                compiler.compileInlineMarkdown(extracted.summaryMarkdown(), details.getSummaryBox());
                if (details.getSummaryBox()
                    .isEmpty()) {
                    details.setFallbackSummaryText("Details");
                }
            }
            compiler.compileBlockMarkdown(extracted.bodyMarkdown(), details.getContentBox());
        } else {
            // Reached for a transcluded <details>, whose body has no source of its own; its parsed children
            // already carry whatever the template substituted, so they are compiled directly.
            compileAstChildren(compiler, details, el.children());
        }

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
        // Without a source slice the parser has given us the body as opaque text, so the summary is still
        // markup inside that text rather than an element; the text is re-read to split it out.
        String summaryMarkdown = leadingSummaryText(children);
        if (summaryMarkdown != null) {
            details.getSummaryBox()
                .clearContent();
            compiler.compileInlineMarkdown(summaryMarkdown, details.getSummaryBox());
            if (details.getSummaryBox()
                .isEmpty()) {
                details.setFallbackSummaryText("Details");
            }
        }

        List<MdAstAnyContent> body = withoutLeadingSummaryText(children, summaryMarkdown != null);
        if (!body.isEmpty()) {
            compiler.compileBlockContextInSourceContext(body, details.getContentBox());
        }
    }

    /** The body with a leading summary line dropped, since that line became the summary. */
    private List<MdAstAnyContent> withoutLeadingSummaryText(List<? extends MdAstAnyContent> children,
        boolean summaryWasFound) {
        if (!summaryWasFound) {
            return new ArrayList<>(children);
        }
        List<MdAstAnyContent> body = new ArrayList<>(children.size());
        boolean skipped = false;
        for (MdAstAnyContent child : children) {
            if (!skipped && isSummaryText(child)) {
                skipped = true;
                continue;
            }
            body.add(child);
        }
        return body;
    }

    /**
     * The markdown inside a leading {@code <summary>} tag, which the parser leaves as literal text when a
     * body has no source slice. Returns null when the body does not start with one.
     */
    private @Nullable String leadingSummaryText(List<? extends MdAstAnyContent> children) {
        for (MdAstAnyContent child : children) {
            String text = textOf(child);
            if (text == null) {
                return null;
            }
            if (text.isBlank()) {
                continue;
            }
            Matcher matcher = SUMMARY_PATTERN.matcher(text);
            // Only a summary that opens the body counts, so a mention later in the text is left alone.
            return matcher.find() && text.substring(0, matcher.start())
                .isBlank() ? matcher.group(1) : null;
        }
        return null;
    }

    private boolean isSummaryText(MdAstAnyContent child) {
        String text = textOf(child);
        if (text == null) {
            return false;
        }
        Matcher matcher = SUMMARY_PATTERN.matcher(text);
        return matcher.find() && text.substring(0, matcher.start())
            .isBlank();
    }

    /** The literal text of a node when it holds nothing but text, or null otherwise. */
    private @Nullable String textOf(MdAstAnyContent node) {
        if (node instanceof MdAstText text) {
            return text.value;
        }
        if (node instanceof MdxJsxElementFields element) {
            StringBuilder builder = new StringBuilder();
            for (MdAstAnyContent nested : element.children()) {
                String nestedText = textOf(nested);
                if (nestedText == null) {
                    return null;
                }
                builder.append(nestedText);
            }
            return builder.toString();
        }
        return null;
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
