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
            if (p.children()
                .isEmpty()) {
                return null;
            }
            if (p.children()
                .getFirst() instanceof MdAstText text) {
                Matcher matcher = TASK_PATTERN.matcher(text.value);
                if (matcher.matches()) {
                    return new TaskMarker(!" ".equals(matcher.group(1)), matcher.group(2), text);
                }
            }
        }
        return null;
    }

    @Desugar
    public record TaskMarker(boolean checked, String remainingText, MdAstText textNode) {}
}
