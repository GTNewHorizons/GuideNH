package com.hfstudio.guidenh.guide.internal.editor.gui;

import com.hfstudio.guidenh.guide.syntax.SyntaxReplacement;

public class EditorTextInsertion {

    private EditorTextInsertion() {}

    public static SyntaxReplacement prepare(String source, int insertionStart, SyntaxReplacement replacement) {
        int start = Math.clamp(insertionStart, 0, source.length());
        int lineStart = source.lastIndexOf('\n', start - 1) + 1;
        int indentEnd = lineStart;
        while (indentEnd < start && (source.charAt(indentEnd) == ' ' || source.charAt(indentEnd) == '\t')) {
            indentEnd++;
        }
        return transform(replacement, source.substring(lineStart, indentEnd));
    }

    public static SyntaxReplacement normalize(SyntaxReplacement replacement) {
        return transform(replacement, "");
    }

    private static SyntaxReplacement transform(SyntaxReplacement replacement, String indent) {
        String text = replacement.text();
        if (text.indexOf('\r') < 0 && (indent.isEmpty() || text.indexOf('\n') < 0)) {
            return replacement;
        }
        StringBuilder result = new StringBuilder(text.length());
        int caret = replacement.caretOffset();
        int selectionEnd = replacement.selectionEndOffset();
        int mappedCaret = 0;
        int mappedSelectionEnd = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == caret) mappedCaret = result.length();
            if (i == selectionEnd) mappedSelectionEnd = result.length();
            if (i == text.length()) break;
            char character = text.charAt(i);
            if (character != '\r' && character != '\n') {
                result.append(character);
                continue;
            }
            boolean crlf = character == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n';
            result.append('\n');
            if (i + (crlf ? 2 : 1) < text.length()) result.append(indent);
            if (crlf) {
                i++;
                if (i == caret) mappedCaret = result.length();
                if (i == selectionEnd) mappedSelectionEnd = result.length();
            }
        }
        return new SyntaxReplacement(result.toString(), mappedCaret, mappedSelectionEnd);
    }
}
