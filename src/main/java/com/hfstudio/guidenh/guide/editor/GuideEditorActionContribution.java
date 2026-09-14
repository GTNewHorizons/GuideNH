package com.hfstudio.guidenh.guide.editor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

public class GuideEditorActionContribution implements Extension {

    /** Declares editor actions for one guide, next to the global registration that adds them to every guide. */
    public interface Provider extends Extension {

        ExtensionPoint<Provider> EXTENSION_POINT = new ExtensionPoint<>(Provider.class);

        List<GuideEditorActionContribution> editorActions();
    }

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

    /** An action that surrounds the selection with markers, or inserts the pair at the caret when nothing is. */
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

    public String label() {
        return label;
    }

    public SceneEditorIcon icon() {
        return icon;
    }

    @Nullable
    public String insertText() {
        return insertText;
    }

    @Nullable
    public String wrapPrefix() {
        return wrapPrefix;
    }

    @Nullable
    public String wrapSuffix() {
        return wrapSuffix;
    }
}
