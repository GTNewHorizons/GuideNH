package com.hfstudio.guidenh.libs.micromark.html;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.hfstudio.guidenh.libs.micromark.Extension;

import lombok.Getter;

@Getter
public class ParseOptions {

    private final List<Extension> extensions = new ArrayList<>();

    public ParseOptions withSyntaxExtension(Extension extension) {
        this.extensions.add(extension);
        return this;
    }

    public ParseOptions withSyntaxExtension(Consumer<Extension> customizer) {
        var extension = new Extension();
        customizer.accept(extension);
        this.extensions.add(extension);
        return this;
    }
}
