package com.hfstudio.guidenh.client.capture.session;

import java.util.ArrayDeque;
import java.util.Deque;

public class CaptureSelectionHistory {

    private static final int LIMIT = 100;
    private final Deque<CaptureSelectionSnapshot> undo = new ArrayDeque<>();
    private final Deque<CaptureSelectionSnapshot> redo = new ArrayDeque<>();

    public void record(CaptureSelectionSnapshot snapshot) {
        if (!snapshot.equals(undo.peekFirst())) {
            undo.addFirst(snapshot);
            if (undo.size() > LIMIT) undo.removeLast();
        }
        redo.clear();
    }

    public CaptureSelectionSnapshot undo(CaptureSelectionSnapshot current) {
        if (undo.isEmpty()) return null;
        redo.addFirst(current);
        return undo.removeFirst();
    }

    public CaptureSelectionSnapshot redo(CaptureSelectionSnapshot current) {
        if (redo.isEmpty()) return null;
        undo.addFirst(current);
        return redo.removeFirst();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }
}
