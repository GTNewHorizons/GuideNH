package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * What a {@link SyntaxSlot} found under the caret: the range an accepted value replaces, what the author
 * already typed inside it, the values the editor offers and how to write an accepted one.
 */
public record SyntaxSlotMatch(int replaceStart, int replaceEnd, String typedText, List<SyntaxSuggestion> suggestions,
    @Nullable SyntaxValueWriter writer) {

    public SyntaxSlotMatch {
        replaceStart = Math.max(0, replaceStart);
        replaceEnd = Math.max(replaceStart, replaceEnd);
        typedText = typedText != null ? typedText : "";
        suggestions = suggestions != null ? List.copyOf(suggestions) : List.of();
    }

    /** A match that replaces its range with the accepted value and puts the caret after it. */
    public static SyntaxSlotMatch replacing(int replaceStart, int replaceEnd, String typedText,
        List<SyntaxSuggestion> suggestions) {
        return new SyntaxSlotMatch(replaceStart, replaceEnd, typedText, suggestions, null);
    }

    /** The text an accepted value replaces, plus the caret and selection position inside the result. */
    public SyntaxReplacement replacementFor(SyntaxSuggestion suggestion) {
        if (writer != null) {
            return writer.write(suggestion);
        }
        String value = suggestion != null && suggestion.value() != null ? suggestion.value() : "";
        return SyntaxReplacement.cursorAtEnd(value);
    }
}
