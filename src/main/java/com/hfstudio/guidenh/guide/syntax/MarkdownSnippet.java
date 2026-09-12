package com.hfstudio.guidenh.guide.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * A snippet the editor offers for markdown syntax.
 */
public record MarkdownSnippet(MarkdownSnippetKind kind, String trigger, String label, String snippet, int caretOffset) {

    /** Characters after which an inline construct may start; a word character must not trigger one. */
    private static final String INLINE_BOUNDARY_CHARS = "([{>\"'";

    public static MarkdownSnippet block(String trigger, String label, String snippet, int caretOffset) {
        return new MarkdownSnippet(MarkdownSnippetKind.BLOCK, trigger, label, snippet, caretOffset);
    }

    public static MarkdownSnippet inline(String trigger, String label, String snippet, int caretOffset) {
        return new MarkdownSnippet(MarkdownSnippetKind.INLINE, trigger, label, snippet, caretOffset);
    }

    /**
     * True when this snippet should be offered for {@code typed}.
     */
    public boolean matches(String typed) {
        String normalized = typed != null ? typed.toLowerCase(Locale.ROOT) : "";
        if (kind.lineLead()) {
            return !normalized.endsWith(" ") && trigger.toLowerCase(Locale.ROOT)
                .startsWith(normalized);
        }
        return trigger.equalsIgnoreCase(normalized);
    }

    /** True when at least one of {@code snippets} matches {@code typed}. */
    public static boolean anyMatches(List<MarkdownSnippet> snippets, String typed) {
        for (MarkdownSnippet snippet : snippets) {
            if (snippet.matches(typed)) {
                return true;
            }
        }
        return false;
    }

    /** All snippets in {@code snippets} that match {@code typed}. */
    public static List<MarkdownSnippet> matching(List<MarkdownSnippet> snippets, String typed) {
        List<MarkdownSnippet> results = new ArrayList<>();
        for (MarkdownSnippet snippet : snippets) {
            if (snippet.matches(typed)) {
                results.add(snippet);
            }
        }
        return results;
    }

    /**
     * The longest inline trigger that {@code text} ends with, requiring the trigger to sit on a word
     * boundary so that prose such as {@code 2*3} or a finished {@code **bold**} does not trigger one.
     */
    @Nullable
    public static MarkdownSnippet longestInlineTrigger(List<MarkdownSnippet> snippets, String text, int cursorIndex) {
        MarkdownSnippet best = null;
        for (MarkdownSnippet snippet : snippets) {
            if (snippet.kind()
                .lineLead()) {
                continue;
            }
            int length = snippet.trigger()
                .length();
            if (length > cursorIndex) {
                continue;
            }
            if (best != null && length <= best.trigger()
                .length()) {
                continue;
            }
            int triggerStart = cursorIndex - length;
            if (!text.regionMatches(true, triggerStart, snippet.trigger(), 0, length)) {
                continue;
            }
            if (!isInlineBoundary(text, triggerStart)) {
                continue;
            }
            best = snippet;
        }
        return best;
    }

    private static boolean isInlineBoundary(String text, int triggerStart) {
        if (triggerStart == 0) {
            return true;
        }
        char before = text.charAt(triggerStart - 1);
        return Character.isWhitespace(before) || INLINE_BOUNDARY_CHARS.indexOf(before) >= 0;
    }
}
