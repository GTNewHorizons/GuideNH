package com.hfstudio.guidenh.guide.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public record MarkdownSnippet(MarkdownSnippetKind kind, String trigger, String label, String snippet, int caretOffset) {

    private static final String INLINE_BOUNDARY_CHARS = "([{>\"'";

    public static MarkdownSnippet block(String trigger, String label, String snippet, int caretOffset) {
        return new MarkdownSnippet(MarkdownSnippetKind.BLOCK, trigger, label, snippet, caretOffset);
    }

    public static MarkdownSnippet inline(String trigger, String label, String snippet, int caretOffset) {
        return new MarkdownSnippet(MarkdownSnippetKind.INLINE, trigger, label, snippet, caretOffset);
    }

    public boolean matches(String typed) {
        String normalized = typed != null ? typed.toLowerCase(Locale.ROOT) : "";
        if (kind.lineLead()) {
            return !normalized.endsWith(" ") && trigger.toLowerCase(Locale.ROOT)
                .startsWith(normalized);
        }
        return trigger.equalsIgnoreCase(normalized);
    }

    public static boolean anyMatches(List<MarkdownSnippet> snippets, String typed) {
        for (MarkdownSnippet snippet : snippets) {
            if (snippet.matches(typed)) {
                return true;
            }
        }
        return false;
    }

    public static List<MarkdownSnippet> matching(List<MarkdownSnippet> snippets, String typed) {
        List<MarkdownSnippet> results = new ArrayList<>();
        for (MarkdownSnippet snippet : snippets) {
            if (snippet.matches(typed)) {
                results.add(snippet);
            }
        }
        return results;
    }

    /** The longest inline trigger that {@code text} ends with, on a word boundary. */
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
