package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.NeiActionContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;
import com.hfstudio.guidenh.guide.internal.editor.gui.EditorTextInsertion;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.syntax.SyntaxReplacement;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;

public class AutocompleteCommitService {

    private AutocompleteCommitService() {}

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
        int swallowedStart = clamp(replaceStart - replacement.extraReplaceStart, 0, replaceEnd);
        int swallowedEnd = clamp(replaceEnd + replacement.extraReplaceEnd, swallowedStart, source.length());
        var rawInsertion = new SyntaxReplacement(
            replacement.text + suffixOf(candidate),
            replacement.cursorOffset,
            replacement.selectionEndOffset);
        var insertion = context instanceof SlotContext || context instanceof MdxValueContext
            || context instanceof NeiActionContext ? EditorTextInsertion.normalize(rawInsertion)
                : EditorTextInsertion.prepare(source, swallowedStart, rawInsertion);
        String inserted = insertion.text();
        if (!(context instanceof SlotContext) && inserted.endsWith("\n")
            && swallowedEnd < source.length()
            && source.charAt(swallowedEnd) == '\n') swallowedEnd++;
        String newText = source.substring(0, swallowedStart) + inserted + source.substring(swallowedEnd);
        int cursor = swallowedStart + insertion.caretOffset();
        int selectionEnd = swallowedStart + insertion.selectionEndOffset();
        return new AutocompleteCommit(newText, cursor, selectionEnd);
    }

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
        if (context instanceof NeiActionContext action) {
            return Replacement.cursorAtEnd(
                action.valueKind() == null ? replacement
                    : URLEncoder.encode(replacement, StandardCharsets.UTF_8)
                        .replace("+", "%20"));
        }
        if (context instanceof TagStartContext tagStart) {
            return createTagReplacement(source, tagStart, candidate);
        }
        if (context instanceof MdxValueContext value) {
            return createAttributeValueReplacement(source, value, replacement, candidate);
        }
        if (context instanceof FrontmatterContext frontmatter) {
            return createFrontmatterReplacement(source, frontmatter, replacement);
        }
        return Replacement.cursorAtEnd(replacement);
    }

    /**
     * A slot of another mod writes its own replacement, so the candidate only has to say which value the author.
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
     * A tag candidate either supplies its own opening form (a container like {@code Row>} that the caller closes.
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
     * True when a candidate writes the whole tag, opening bracket included, while the page already has a bracket.
     */
    private static boolean swallowsOpeningBracket(String source, int replaceStart, String replacement) {
        return replacement.startsWith("<") && replaceStart > 0 && source.charAt(replaceStart - 1) == '<';
    }

    private static boolean bringsCompleteForm(AutocompleteCandidate candidate) {
        return candidate.suffixText() != null || candidate.caretOffsetInReplacement() >= 0;
    }

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
     * Writes an attribute value, adding quotes when the page does not quote it yet and closing a half-typed.
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
        } else {
            replacement = EditorTextInsertion.normalize(SyntaxReplacement.cursorAtEnd(replacement))
                .text();
            int start = clamp(context.replaceStart(), 0, source.length());
            int lineStart = source.lastIndexOf('\n', start - 1) + 1;
            if (replacement.startsWith("\n") && source.substring(lineStart, start)
                .isBlank()) {
                replacement = removeBlockIndent(replacement.substring(1));
            } else if (replacement.startsWith("\n") && endsWithListMarker(source, start)) {
                replacement = stripListMarkerLine(removeBlockIndent(replacement.substring(1)));
            }
        }
        return new Replacement(replacement, replacement.length(), replacement.length());
    }

    private static String removeBlockIndent(String text) {
        int indent = 0;
        while (indent < text.length() && (text.charAt(indent) == ' ' || text.charAt(indent) == '\t')) indent++;
        if (indent == 0) return text;
        String prefix = text.substring(0, indent);
        StringBuilder result = new StringBuilder(text.length());
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) end = text.length();
            int contentStart = text.startsWith(prefix, start) ? start + indent : start;
            result.append(text, contentStart, end);
            if (end < text.length()) result.append('\n');
            start = end + 1;
        }
        return result.toString();
    }

    private static boolean endsWithListMarker(String source, int position) {
        int lineStart = source.lastIndexOf('\n', position - 1) + 1;
        if (lineStart >= position) {
            return false;
        }
        String before = source.substring(lineStart, position)
            .trim();
        return before.equals("-") || before.equals("+") || before.equals("*");
    }

    private static String stripListMarkerLine(String value) {
        int index = skipSpaces(value, 0);
        if (index < value.length()
            && (value.charAt(index) == '-' || value.charAt(index) == '+' || value.charAt(index) == '*')) {
            index++;
        }
        return value.substring(skipSpaces(value, index));
    }

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
        while (pos < source.length() && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
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
