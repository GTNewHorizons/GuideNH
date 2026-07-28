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

    /**
     * Detects whether the given {@code <li>} children contain a GFM task-list
     * marker ({@code [x]} / {@code [ ]}) in the first paragraph.
     *
     * <p>This method is PURE DETECTION — it does NOT mutate the AST tree.
     * The returned {@link TaskMarker} carries the prefix length so callers
     * can strip the prefix for display without permanently altering the AST.
     * This is critical because the same {@code ParsedGuidePage} (and its AST)
     * may be compiled multiple times (e.g. by {@code CompileWorker} then by
     * {@code RenderPageService}); mutation would cause the second compile to
     * miss the marker.
     *
     * @param children the {@code <li>} element's children
     * @return a {@link TaskMarker} describing the detected marker, or {@code null}
     */
    public static @Nullable TaskMarker extractTaskMarker(List<? extends MdAstAnyContent> children) {
        // Find the first <p> child (nested <li> may have [<p>, <ul>])
        MdxJsxFlowElement p = null;
        for (var child : children) {
            if (child instanceof MdxJsxFlowElement el && "p".equals(el.name())) {
                p = el;
                break;
            }
        }
        if (p == null) {
            return null;
        }

        var pChildren = p.children();
        if (pChildren.isEmpty()) {
            return null;
        }

        // Use recursive toText() to capture full text across inline formatting
        // (e.g. "[x] **Bold** task" has text nodes only for "[x] " and " task",
        // with the "Bold" inside a non-text MdxJsxTextElement).
        String fullText = p.toText();

        Matcher matcher = TASK_PATTERN.matcher(fullText);
        if (!matcher.matches()) {
            return null;
        }

        boolean checked = !" ".equals(matcher.group(1));
        // remainingText is computed from the regex; the AST text nodes are NOT modified.
        String remainingText = matcher.group(2);
        int prefixLen = fullText.length() - remainingText.length();

        // Find the first text node (for callers to locate the text to strip).
        MdAstText firstTextNode = null;
        for (Object child : pChildren) {
            if (child instanceof MdAstText text) {
                firstTextNode = text;
                break;
            }
        }

        if (firstTextNode == null) {
            return null;
        }

        return new TaskMarker(checked, remainingText, firstTextNode, prefixLen);
    }

    /**
     * Strips the task-marker prefix from the first {@code <p>} child's text
     * nodes in-place. Callers MUST save original values beforehand and restore
     * them after compilation to keep the AST immutable.
     *
     * @param p          the {@code <p>} element whose text children to modify
     * @param prefixLen  number of characters to strip from the start
     * @return a list of original text values in order (for later restoration)
     */
    public static List<String> stripPrefixInPlace(MdxJsxFlowElement p, int prefixLen) {
        var saved = new java.util.ArrayList<String>();
        int remainingToStrip = prefixLen;
        for (Object child : p.children()) {
            if (child instanceof MdAstText text) {
                saved.add(text.value);
                if (remainingToStrip > 0) {
                    int stripFromThis = Math.min(remainingToStrip, text.value.length());
                    text.setValue(text.value.substring(stripFromThis));
                    remainingToStrip -= stripFromThis;
                }
            }
        }
        return saved;
    }

    /**
     * Restores text nodes to their original values after a temporary
     * {@link #stripPrefixInPlace} call.
     */
    public static void restoreTextValues(MdxJsxFlowElement p, List<String> savedValues) {
        int idx = 0;
        for (Object child : p.children()) {
            if (child instanceof MdAstText text && idx < savedValues.size()) {
                text.setValue(savedValues.get(idx++));
            }
        }
    }

    /** Finds the first {@code <p>} child in an {@code <li>}'s children. */
    @Nullable
    public static MdxJsxFlowElement findFirstP(List<? extends MdAstAnyContent> children) {
        for (var child : children) {
            if (child instanceof MdxJsxFlowElement el && "p".equals(el.name())) {
                return el;
            }
        }
        return null;
    }

    @Desugar
    public record TaskMarker(boolean checked, String remainingText, MdAstText textNode, int prefixLen) {}
}
