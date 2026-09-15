package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import java.util.List;

public record CodeHighlightResult(String languageId, CodeHighlightMode mode, List<CodeHighlightLine> lines) {

    public boolean isPlain() {
        return mode == CodeHighlightMode.PLAIN;
    }
}
