package com.hfstudio.guidenh.guide.internal.editor.reference;

import java.util.List;

public record GuideEditorReferenceSection(String documentId, String documentTitle, String title, int depth,
    String markdown, String searchText, List<String> anchors) {

    public GuideEditorReferenceSection {
        anchors = List.copyOf(anchors);
    }
}
