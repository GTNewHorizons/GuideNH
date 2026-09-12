package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.syntax.SyntaxReplacement;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;

/**
 * Applies an accepted candidate to the page text.
 */
public class AutocompleteCommitService {

    private AutocompleteCommitService() {}

    /**
     * Applies an accepted candidate to the page text.
     *
     * @return the edit to apply, or null when the candidate could not produce one
     */
    @Nullable
    public static AutocompleteCommit commit(String text, AutocompleteContext context, AutocompleteCandidate candidate) {
        String source = text != null ? text : "";
        int replaceStart = clamp(context.replaceStart(), 0, source.length());
        int replaceEnd = clamp(context.replaceEnd(), replaceStart, source.length());
        Replacement replacement = createReplacement(source, context, candidate);
        if (replacement == null) {
            return null;
        }
        replacement = applyDeclaredCaret(replacement, candidate);
        String inserted = replacement.text + suffixOf(candidate);
        int swallowedStart = clamp(replaceStart - replacement.extraReplaceStart, 0, replaceEnd);
        int swallowedEnd = clamp(replaceEnd + replacement.extraReplaceEnd, swallowedStart, source.length());
        String newText = source.substring(0, swallowedStart) + inserted + source.substring(swallowedEnd);
        int cursor = swallowedStart + replacement.cursorOffset;
        int selectionEnd = swallowedStart + replacement.selectionEndOffset;
        return new AutocompleteCommit(newText, cursor, selectionEnd);
    }

    /** Honours a candidate that knows where the caret and the selection belong inside its replacement. */
    private static Replacement applyDeclaredCaret(Replacement replacement, AutocompleteCandidate candidate) {
        int caret = candidate.caretOffsetInReplacement();
        if (caret < 0 || caret > replacement.text.length()) {
            return replacement;
        }
        int selectionEnd = candidate.selectionEndInReplacement();
        return new Replacement(
            replacement.text,
            caret,
            selectionEnd >= 0 ? selectionEnd : caret,
            replacement.extraReplaceStart,
            replacement.extraReplaceEnd);
    }

    private static String suffixOf(AutocompleteCandidate candidate) {
        String suffix = candidate.suffixText();
        return suffix != null ? suffix : "";
    }

    private static Replacement createReplacement(String source, AutocompleteContext context,
        AutocompleteCandidate candidate) {
        if (context instanceof SlotContext slot) {
            return createSlotReplacement(slot, candidate);
        }
        String replacement = candidate.replacementText() != null ? candidate.replacementText() : "";
        if (context instanceof TagStartContext tagStart) {
            return createTagReplacement(source, tagStart, candidate);
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
     * A slot of another mod writes its own replacement, so the candidate only has to say which value the
     * author picked; the slot decides the surroundings and where the caret lands.
     */
    @Nullable
    private static Replacement createSlotReplacement(SlotContext context, AutocompleteCandidate candidate) {
        SyntaxSuggestion suggestion = candidate.syntaxSuggestion();
        if (suggestion == null) {
            String text = candidate.replacementText() != null ? candidate.replacementText() : "";
            suggestion = SyntaxSuggestion.of(text);
        }
        SyntaxReplacement replacement;
        try {
            replacement = context.match()
                .replacementFor(suggestion);
        } catch (RuntimeException e) {
            GuideDebugLog
                .error("[GuideNH] [SyntaxSlot] {} failed to write a value: {}", context.slotNamespace(), e.toString());
            return null;
        }
        if (replacement == null) {
            GuideDebugLog.error(
                "[GuideNH] [SyntaxSlot] {} answered no replacement for '{}'",
                context.slotNamespace(),
                suggestion.value());
            return null;
        }
        return new Replacement(replacement.text(), replacement.caretOffset(), replacement.selectionEndOffset());
    }

    /**
     * A tag candidate either supplies its own opening form (a container like {@code Row>} that the
     * caller closes through {@link AutocompleteCandidate#suffixText()}, or a template) or is a plain name
     * that becomes the self-closing {@code <Name />}.
     */
    private static Replacement createTagReplacement(String source, TagStartContext context,
        AutocompleteCandidate candidate) {
        String tagName = candidate.replacementText() != null ? candidate.replacementText() : "";
        int replaceStart = clamp(context.replaceStart(), 0, source.length());
        int replaceEnd = clamp(context.replaceEnd(), replaceStart, source.length());
        int extraReplaceStart = swallowsOpeningBracket(source, replaceStart, tagName) ? 1 : 0;
        int pos = skipSpaces(source, replaceEnd);
        int closingEnd = closingBracketEnd(source, pos);
        if (closingEnd > 0 && bringsCompleteForm(candidate)) {
            // The author already typed the end of this tag, so the complete form replaces that too instead
            // of leaving a second bracket behind.
            return new Replacement(
                tagName,
                tagName.length(),
                tagName.length(),
                extraReplaceStart,
                closingEnd - replaceEnd);
        }
        if (pos < source.length()) {
            char next = source.charAt(pos);
            if (next == '>' || next == '/') {
                return new Replacement(tagName, tagName.length(), tagName.length(), extraReplaceStart, 0);
            }
        }
        if (tagName.endsWith(">")) {
            return new Replacement(tagName, tagName.length(), tagName.length(), extraReplaceStart, 0);
        }
        String text = tagName + " />";
        return new Replacement(text, text.length() - 2, text.length() - 2, extraReplaceStart, 0);
    }

    /**
     * True when a candidate writes the whole tag, opening bracket included, while the page already has a
     * bracket just before the typed name: the bracket belongs to the replacement, so it is replaced too
     * instead of staying behind as a second one.
     */
    private static boolean swallowsOpeningBracket(String source, int replaceStart, String replacement) {
        return replacement.startsWith("<") && replaceStart > 0 && source.charAt(replaceStart - 1) == '<';
    }

    /** True when a candidate brings its own complete tag form rather than a bare name. */
    private static boolean bringsCompleteForm(AutocompleteCandidate candidate) {
        return candidate.suffixText() != null || candidate.caretOffsetInReplacement() >= 0;
    }

    /** @return the end of a closing bracket at {@code position}, or -1 when the text there is something else */
    private static int closingBracketEnd(String source, int position) {
        if (position < 0 || position >= source.length()
            || source.charAt(position) != '>' && source.charAt(position) != '/') {
            return -1;
        }
        if (source.charAt(position) == '>') {
            return position + 1;
        }
        return position + 1 < source.length() && source.charAt(position + 1) == '>' ? position + 2 : -1;
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
        private final int extraReplaceStart;
        private final int extraReplaceEnd;

        private Replacement(String text, int cursorOffset, int selectionEndOffset) {
            this(text, cursorOffset, selectionEndOffset, 0, 0);
        }

        private Replacement(String text, int cursorOffset, int selectionEndOffset, int extraReplaceEnd) {
            this(text, cursorOffset, selectionEndOffset, 0, extraReplaceEnd);
        }

        /**
         * @param extraReplaceStart characters before the context's range the replacement also consumes,
         *                          used when the candidate writes a whole tag including its bracket
         * @param extraReplaceEnd   characters after it the replacement also consumes, used when the author
         *                          already typed the end of a tag
         */
        private Replacement(String text, int cursorOffset, int selectionEndOffset, int extraReplaceStart,
            int extraReplaceEnd) {
            this.text = text;
            this.cursorOffset = cursorOffset;
            this.selectionEndOffset = selectionEndOffset;
            this.extraReplaceStart = extraReplaceStart;
            this.extraReplaceEnd = extraReplaceEnd;
        }

        private static Replacement cursorAtEnd(String text) {
            return new Replacement(text, text.length(), text.length());
        }
    }
}
