package com.hfstudio.guidenh.libs.micromark.html;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.Setter;

public class CompileOptions {

    @Getter
    @Setter
    private List<HtmlExtension> extensions = new ArrayList<>();

    @Nullable
    private String defaultLineEnding;

    @Getter
    @Setter
    private boolean allowDangerousHtml;

    @Getter
    @Setter
    private boolean allowDangerousProtocol;

    public @Nullable String getDefaultLineEnding() {
        return defaultLineEnding;
    }

    public void setDefaultLineEnding(@Nullable String defaultLineEnding) {
        this.defaultLineEnding = defaultLineEnding;
    }

    public CompileOptions allowDangerousHtml() {
        this.allowDangerousHtml = true;
        return this;
    }

    public CompileOptions allowDangerousProtocol() {
        this.allowDangerousProtocol = true;
        return this;
    }

    public CompileOptions withExtension(HtmlExtension extension) {
        extensions.add(extension);
        return this;
    }
}
