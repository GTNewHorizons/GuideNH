package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class SceneEditorTextSelectionModel {

    @Getter
    private String text;
    @Getter
    private int cursorIndex;
    private int selectionAnchor;
    private boolean selectionActive;

    public SceneEditorTextSelectionModel() {
        this.text = "";
        this.cursorIndex = 0;
        this.selectionAnchor = 0;
        this.selectionActive = false;
    }

    public void setText(String text) {
        this.text = normalizeLineEndings(text);
        this.cursorIndex = Math.min(cursorIndex, this.text.length());
        if (selectionActive) {
            this.selectionAnchor = Math.min(selectionAnchor, this.text.length());
            if (selectionAnchor == cursorIndex) {
                selectionActive = false;
            }
        }
    }

    public void setCursorIndex(int cursorIndex) {
        this.cursorIndex = clampIndex(cursorIndex);
        this.selectionAnchor = this.cursorIndex;
        this.selectionActive = false;
    }

    public void setSelection(int selectionStart, int selectionEnd) {
        int start = clampIndex(selectionStart);
        int end = clampIndex(selectionEnd);
        this.selectionAnchor = start;
        this.cursorIndex = end;
        this.selectionActive = start != end;
    }

    public void beginSelection(int anchorIndex) {
        this.selectionAnchor = clampIndex(anchorIndex);
        this.cursorIndex = this.selectionAnchor;
        this.selectionActive = false;
    }

    public void updateSelection(int cursorIndex) {
        this.cursorIndex = clampIndex(cursorIndex);
        this.selectionActive = this.cursorIndex != this.selectionAnchor;
    }

    public void moveCursor(int cursorIndex, boolean keepSelection) {
        int clamped = clampIndex(cursorIndex);
        if (keepSelection) {
            if (!selectionActive) {
                selectionAnchor = this.cursorIndex;
            }
            this.cursorIndex = clamped;
            this.selectionActive = this.cursorIndex != selectionAnchor;
            return;
        }
        this.cursorIndex = clamped;
        this.selectionAnchor = clamped;
        this.selectionActive = false;
    }

    public boolean hasSelection() {
        return selectionActive && getSelectionStart() != getSelectionEnd();
    }

    public int getSelectionStart() {
        return Math.min(selectionAnchor, cursorIndex);
    }

    public int getSelectionEnd() {
        return Math.max(selectionAnchor, cursorIndex);
    }

    public String getSelectedText() {
        if (!hasSelection()) {
            return "";
        }
        return text.substring(getSelectionStart(), getSelectionEnd());
    }

    public void selectAll() {
        selectionAnchor = 0;
        cursorIndex = text.length();
        selectionActive = !text.isEmpty();
    }

    /** Selects the token around the cursor, including resource ids and paths. */
    public void selectWordAt(int index) {
        int position = clampIndex(index);
        if (position > 0 && (position == text.length() || !isWordCharacter(text.charAt(position)))
            && isWordCharacter(text.charAt(position - 1))) {
            position--;
        }
        if (position >= text.length() || !isWordCharacter(text.charAt(position))) {
            setCursorIndex(position);
            return;
        }
        int start = position;
        int end = position + 1;
        while (start > 0 && isWordCharacter(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && isWordCharacter(text.charAt(end))) {
            end++;
        }
        setSelection(start, end);
    }

    /** Indents or outdents every logical line touched by the current selection. */
    public boolean indentLines(boolean outdent, int indentSize) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (!hasSelection()) {
            start = lineStart(start);
            end = start;
            while (end < text.length() && text.charAt(end) != '\n') {
                end++;
            }
        } else {
            start = lineStart(start);
            if (end > 0 && end == lineStart(end) && end > start) {
                end--;
            }
            end = lineEnd(end);
        }

        List<LineEdit> edits = new ArrayList<>();
        int line = start;
        while (line <= end) {
            int removed = 0;
            if (outdent) {
                while (removed < indentSize && line + removed < text.length()) {
                    char c = text.charAt(line + removed);
                    if (c == ' ') {
                        removed++;
                    } else if (c == '\t' && removed == 0) {
                        removed = 1;
                        break;
                    } else {
                        break;
                    }
                }
            }
            int added = outdent ? -removed : indentSize;
            if (added != 0) {
                edits.add(new LineEdit(line, removed, added));
            }
            int next = text.indexOf('\n', line);
            if (next < 0 || next >= end) {
                break;
            }
            line = next + 1;
        }
        if (edits.isEmpty()) {
            return false;
        }

        StringBuilder updated = new StringBuilder(text.length());
        int source = 0;
        for (LineEdit edit : edits) {
            updated.append(text, source, edit.offset);
            if (edit.added > 0) {
                for (int i = 0; i < edit.added; i++) {
                    updated.append(' ');
                }
            }
            source = edit.offset + edit.removed;
        }
        updated.append(text, source, text.length());
        text = updated.toString();
        selectionAnchor = adjustIndex(selectionAnchor, edits, text.length());
        cursorIndex = adjustIndex(cursorIndex, edits, text.length());
        selectionActive = selectionAnchor != cursorIndex;
        return true;
    }

    private int lineStart(int index) {
        int cursor = clampIndex(index);
        int newline = text.lastIndexOf('\n', Math.max(0, cursor - 1));
        return newline < 0 ? 0 : newline + 1;
    }

    private int lineEnd(int index) {
        int newline = text.indexOf('\n', clampIndex(index));
        return newline < 0 ? text.length() : newline;
    }

    private int adjustIndex(int index, List<LineEdit> edits, int newTextLength) {
        int adjusted = index;
        for (LineEdit edit : edits) {
            if (index >= edit.offset + edit.removed) {
                adjusted += edit.added;
            } else if (index >= edit.offset) {
                adjusted = edit.offset + Math.max(0, edit.added);
                break;
            } else {
                break;
            }
        }
        return Math.max(0, Math.min(adjusted, newTextLength));
    }

    private boolean isWordCharacter(char c) {
        return !Character.isWhitespace(c) && "\"'`()[]{}<>".indexOf(c) < 0;
    }

    private static class LineEdit {

        private final int offset;
        private final int removed;
        private final int added;

        private LineEdit(int offset, int removed, int added) {
            this.offset = offset;
            this.removed = removed;
            this.added = added;
        }
    }

    public void insertText(String insertion) {
        String replacement = normalizeLineEndings(insertion);
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (hasSelection()) {
            text = text.substring(0, start) + replacement + text.substring(end);
            cursorIndex = start + replacement.length();
        } else {
            text = text.substring(0, cursorIndex) + replacement + text.substring(cursorIndex);
            cursorIndex += replacement.length();
        }
        selectionAnchor = cursorIndex;
        selectionActive = false;
    }

    private String normalizeLineEndings(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.indexOf('\r') < 0) {
            return text;
        }
        return text.replace("\r\n", "\n")
            .replace('\r', '\n');
    }

    public String cutSelection() {
        String selected = getSelectedText();
        if (selected.isEmpty()) {
            return "";
        }
        deleteSelection();
        return selected;
    }

    public void deleteBackward() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursorIndex <= 0) {
            return;
        }
        text = text.substring(0, cursorIndex - 1) + text.substring(cursorIndex);
        cursorIndex--;
        selectionAnchor = cursorIndex;
    }

    public void deleteForward() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursorIndex >= text.length()) {
            return;
        }
        text = text.substring(0, cursorIndex) + text.substring(cursorIndex + 1);
        selectionAnchor = cursorIndex;
    }

    private void deleteSelection() {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        text = text.substring(0, start) + text.substring(end);
        cursorIndex = start;
        selectionAnchor = start;
        selectionActive = false;
    }

    private int clampIndex(int index) {
        if (index < 0) {
            return 0;
        }
        return Math.min(index, text.length());
    }
}
