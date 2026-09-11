package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;

/**
 * Applies an accepted candidate to the page text.
 *
 * <p>
 * The context decides the shape of the slot - a tag, an attribute value, a frontmatter entry - while the
 * candidate decides the snippet written into it: its text, where the caret lands, what gets selected and
 * whether the value needs quotes. No tag or attribute knowledge lives here.
 */
public class AutocompleteCommitService {

    private AutocompleteCommitService() {}

    public static AutocompleteCommit commit(String text, AutocompleteContext context, AutocompleteCandidate candidate) {
        String source = text != null ? text : "";
        int replaceStart = clamp(context.replaceStart(), 0, source.length());
        int replaceEnd = clamp(context.replaceEnd(), replaceStart, source.length());
        Replacement replacement = createReplacement(source, context, candidate);
        replacement = applyDeclaredCaret(replacement, candidate);
        String inserted = replacement.text + suffixOf(candidate);
        String newText = source.substring(0, replaceStart) + inserted + source.substring(replaceEnd);
        int cursor = replaceStart + replacement.cursorOffset;
        int selectionEnd = replaceStart + replacement.selectionEndOffset;
        return new AutocompleteCommit(newText, cursor, selectionEnd);
    }

    /** Honours a candidate that knows where the caret and the selection belong inside its replacement. */
    private static Replacement applyDeclaredCaret(Replacement replacement, AutocompleteCandidate candidate) {
        int caret = candidate.caretOffsetInReplacement();
        if (caret < 0 || caret > replacement.text.length()) {
            return replacement;
        }
        int selectionEnd = candidate.selectionEndInReplacement();
        return new Replacement(replacement.text, caret, selectionEnd >= 0 ? selectionEnd : caret);
    }

    private static String suffixOf(AutocompleteCandidate candidate) {
        String suffix = candidate.suffixText();
        return suffix != null ? suffix : "";
    }

    private static Replacement createReplacement(String source, AutocompleteContext context,
        AutocompleteCandidate candidate) {
        String replacement = candidate.replacementText() != null ? candidate.replacementText() : "";
        if (context instanceof TagStartContext tagStart) {
            return createTagReplacement(source, tagStart, replacement);
        }
        if (context instanceof MdxValueContext value) {
            return createAttributeValueReplacement(source, value, replacement, candidate);
        }
        if (context instanceof FrontmatterContext frontmatter) {
            return createFrontmatterReplacement(source, frontmatter, replacement);
        }
        // Attribute names and markdown snippets already are complete snippets.
        return Replacement.cursorAtEnd(replacement);
    }

    /**
     * A tag candidate either supplies its own opening form (a container like {@code Row>} that the
     * caller closes through {@link AutocompleteCandidate#suffixText()}) or is a plain name that becomes
     * the self-closing {@code <Name />}.
     */
    private static Replacement createTagReplacement(String source, TagStartContext context, String tagName) {
        int replaceEnd = clamp(context.replaceEnd(), 0, source.length());
        int pos = skipSpaces(source, replaceEnd);
        if (pos < source.length()) {
            char next = source.charAt(pos);
            if (next == '>' || next == '/') {
                return Replacement.cursorAtEnd(tagName);
            }
        }
        if (tagName.endsWith(">")) {
            return Replacement.cursorAtEnd(tagName);
        }
        String text = tagName + " />";
        return new Replacement(text, text.length() - 2, text.length() - 2);
    }

    /**
     * Writes an attribute value, adding quotes when the page does not quote it yet and closing a
     * half-typed delimiter the resolver detected.
     */
    private static Replacement createAttributeValueReplacement(String source, MdxValueContext context, String value,
        AutocompleteCandidate candidate) {
        int replaceStart = clamp(context.replaceStart(), 0, source.length());
        int replaceEnd = clamp(context.replaceEnd(), replaceStart, source.length());
        if (!hasValueDelimiter(source, replaceStart, replaceEnd) && candidate.quotesValue()) {
            String quoted = "\"" + value + "\"";
            return Replacement.cursorAtEnd(quoted);
        }
        char terminator = context.getMissingValueTerminator();
        if (terminator != '\0' && !endsWith(value, terminator)) {
            String closed = value + terminator;
            return new Replacement(closed, closed.length() - 1, closed.length() - 1);
        }
        return Replacement.cursorAtEnd(value);
    }

    private static Replacement createFrontmatterReplacement(String source, FrontmatterContext context, String rawText) {
        String replacement = rawText != null ? rawText : "";
        if (!context.isValue()) {
            replacement += ": ";
        } else if (!replacement.isEmpty() && replacement.charAt(0) == '\n') {
            int start = context.replaceStart();
            if (start > 0 && source.charAt(start - 1) == '\n') {
                replacement = replacement.substring(1);
            } else if (endsWithListMarker(source, start)) {
                // The line already carries its list marker, so the value must not add a second one.
                replacement = stripListMarkerLine(replacement.substring(1));
            }
        }
        return new Replacement(replacement, replacement.length(), replacement.length());
    }

    /** True when only a list marker and whitespace precede {@code position} on its line. */
    private static boolean endsWithListMarker(String source, int position) {
        int lineStart = source.lastIndexOf('\n', position - 1) + 1;
        if (lineStart >= position) {
            return false;
        }
        String before = source.substring(lineStart, position)
            .trim();
        return before.equals("-") || before.equals("+") || before.equals("*");
    }

    /** Drops the indentation and the list marker a multiline frontmatter value starts with. */
    private static String stripListMarkerLine(String value) {
        int index = skipSpaces(value, 0);
        if (index < value.length()
            && (value.charAt(index) == '-' || value.charAt(index) == '+' || value.charAt(index) == '*')) {
            index++;
        }
        return value.substring(skipSpaces(value, index));
    }

    /** True when the character just outside the replaced range already delimits the value. */
    private static boolean hasValueDelimiter(String source, int valueStart, int valueEnd) {
        int before = valueStart - 1;
        if (before >= 0) {
            char open = source.charAt(before);
            if (open == '"' || open == '\'' || open == '{') {
                return true;
            }
        }
        if (valueEnd < source.length()) {
            char close = source.charAt(valueEnd);
            if (close == '"' || close == '\'' || close == '}') {
                return true;
            }
        }
        return false;
    }

    private static int skipSpaces(String source, int start) {
        int pos = start;
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static boolean endsWith(String value, char suffix) {
        return !value.isEmpty() && value.charAt(value.length() - 1) == suffix;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private static class Replacement {

        private final String text;
        private final int cursorOffset;
        private final int selectionEndOffset;

        private Replacement(String text, int cursorOffset, int selectionEndOffset) {
            this.text = text;
            this.cursorOffset = cursorOffset;
            this.selectionEndOffset = selectionEndOffset;
        }

        private static Replacement cursorAtEnd(String text) {
            return new Replacement(text, text.length(), text.length());
        }
    }
}
