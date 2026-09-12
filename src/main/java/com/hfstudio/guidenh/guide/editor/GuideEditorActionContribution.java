package com.hfstudio.guidenh.guide.editor;

import org.jetbrains.annotations.Nullable;

/**
 * A toolbar or menu entry another mod adds to the guide editor. It carries text rather than behaviour, so
 * the editor applies it itself: {@link #insert} writes text at the caret, {@link #wrap} puts a selection
 * inside markers. Register through {@code GuideNhIntegrationRegistry.registerEditorAction(action)}.
 */
public class GuideEditorActionContribution {

    private final String label;
    private final SceneEditorIcon icon;
    @Nullable
    private final String insertText;
    @Nullable
    private final String wrapPrefix;
    @Nullable
    private final String wrapSuffix;

    private GuideEditorActionContribution(String label, SceneEditorIcon icon, @Nullable String insertText,
        @Nullable String wrapPrefix, @Nullable String wrapSuffix) {
        this.label = label;
        this.icon = icon;
        this.insertText = insertText;
        this.wrapPrefix = wrapPrefix;
        this.wrapSuffix = wrapSuffix;
    }

    /**
     * An action that writes text at the caret, replacing the selection when there is one.
     */
    public static GuideEditorActionContribution insert(String label, SceneEditorIcon icon, String text) {
        requireText(label, "label");
        if (icon == null) {
            throw new IllegalArgumentException("icon");
        }
        if (text == null) {
            throw new IllegalArgumentException("text");
        }
        return new GuideEditorActionContribution(label, icon, text, null, null);
    }

    /**
     * An action that surrounds the selection with markers, or inserts the pair at the caret when nothing is
     * selected.
     */
    public static GuideEditorActionContribution wrap(String label, SceneEditorIcon icon, String prefix, String suffix) {
        requireText(label, "label");
        if (icon == null) {
            throw new IllegalArgumentException("icon");
        }
        if (prefix == null || suffix == null) {
            throw new IllegalArgumentException("prefix and suffix are required");
        }
        return new GuideEditorActionContribution(label, icon, null, prefix, suffix);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.trim()
            .isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    /** Name shown in the toolbar and the context menu. */
    public String label() {
        return label;
    }

    /** Sprite the toolbar draws for this action. */
    public SceneEditorIcon icon() {
        return icon;
    }

    /** Text written at the caret, or null when this action wraps a selection instead. */
    @Nullable
    public String insertText() {
        return insertText;
    }

    /** Text written before a selection, or null when this action inserts text instead. */
    @Nullable
    public String wrapPrefix() {
        return wrapPrefix;
    }

    /** Text written after a selection, or null when this action inserts text instead. */
    @Nullable
    public String wrapSuffix() {
        return wrapSuffix;
    }
}
