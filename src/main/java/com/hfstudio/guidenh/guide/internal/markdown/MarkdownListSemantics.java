package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class MarkdownListSemantics {

    private static final Pattern TASK_PATTERN = Pattern.compile("^\\[( |x|X)]\\s+(.*)$");

    private MarkdownListSemantics() {}

    public static @Nullable TaskMarker extractTaskMarker(List<? extends MdAstAnyContent> children) {
        if (children.size() != 1) {
            return null;
        }
        MdAstAnyContent firstChild = children.getFirst();
        // Post-conversion: <p> element wrapping the task text
        if (firstChild instanceof MdxJsxFlowElement p && "p".equals(p.name())) {
            var pChildren = p.children();
            if (pChildren.isEmpty()) {
                return null;
            }

            // Build full text by concatenating all MdAstText children (micromark label
            // resolution may split "[x]" across multiple text nodes)
            StringBuilder fullText = new StringBuilder();
            for (Object child : pChildren) {
                if (child instanceof MdAstText text) {
                    fullText.append(text.value);
                }
            }

            if (fullText.isEmpty()) {
                return null;
            }

            Matcher matcher = TASK_PATTERN.matcher(fullText);
            if (!matcher.matches()) {
                return null;
            }

            boolean checked = !" ".equals(matcher.group(1));
            int prefixLen = fullText.length() - matcher.group(2).length();

            // Strip the task prefix from text children, spanning multiple nodes if needed
            int remainingToStrip = prefixLen;
            MdAstText firstTextNode = null;
            for (Object child : pChildren) {
                if (child instanceof MdAstText text) {
                    if (firstTextNode == null) {
                        firstTextNode = text;
                    }
                    if (remainingToStrip > 0) {
                        int stripFromThis = Math.min(remainingToStrip, text.value.length());
                        text.setValue(text.value.substring(stripFromThis));
                        remainingToStrip -= stripFromThis;
                    }
                }
            }

            if (firstTextNode == null) {
                return null;
            }

            return new TaskMarker(checked, firstTextNode.value, firstTextNode);
        }
        return null;
    }

    @Desugar
    public record TaskMarker(boolean checked, String remainingText, MdAstText textNode) {}
}
