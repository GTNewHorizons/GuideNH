package com.hfstudio.guidenh.guide.internal.editor.md;

import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

public interface SceneEditorMarkdownParseResult {

    record Success(SceneEditorSceneModel model) implements SceneEditorMarkdownParseResult {}

    record SyntaxError(String message) implements SceneEditorMarkdownParseResult {}

    record Unsupported(String message) implements SceneEditorMarkdownParseResult {}
}
