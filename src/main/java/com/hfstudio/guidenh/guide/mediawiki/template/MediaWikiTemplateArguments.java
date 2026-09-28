package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/** The arguments of one {@code <Template>} call. */
public final class MediaWikiTemplateArguments {

    private Map<String, MediaWikiTemplateValue> positional;
    private Map<String, MediaWikiTemplateValue> named;
    private String signature;

    public void putPositional(int position, MediaWikiTemplateValue value) {
        String key = String.valueOf(position);
        if (positional == null) {
            positional = new LinkedHashMap<>();
        }
        positional.putIfAbsent(key, value);
        signature = null;
    }

    public void putNamed(String name, MediaWikiTemplateValue value) {
        String key = MediaWikiTemplateName.normalize(name);
        if (named == null) {
            named = new LinkedHashMap<>();
        }
        named.putIfAbsent(key, value);
        signature = null;
    }

    public @Nullable MediaWikiTemplateValue get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        MediaWikiTemplateValue positionalValue = positional == null ? null : positional.get(key);
        if (positionalValue != null) {
            return positionalValue;
        }
        if (named == null) {
            return null;
        }
        MediaWikiTemplateValue namedValue = named.get(key);
        return namedValue != null ? namedValue : named.get(MediaWikiTemplateName.normalize(key));
    }

    public boolean isEmpty() {
        return positional == null && named == null;
    }

    public List<String> namedKeys() {
        return named == null ? List.of() : List.copyOf(named.keySet());
    }

    public String signature() {
        if (signature != null) {
            return signature;
        }
        StringBuilder builder = new StringBuilder();
        appendSignature(builder, positional);
        appendSignature(builder, named);
        signature = builder.toString();
        return signature;
    }

    public MediaWikiTemplateArguments copy() {
        MediaWikiTemplateArguments copy = new MediaWikiTemplateArguments();
        if (positional != null) {
            for (Map.Entry<String, MediaWikiTemplateValue> entry : positional.entrySet()) {
                copy.putPositional(
                    Integer.parseInt(entry.getKey()),
                    entry.getValue()
                        .copy());
            }
        }
        if (named != null) {
            for (Map.Entry<String, MediaWikiTemplateValue> entry : named.entrySet()) {
                copy.putNamed(
                    entry.getKey(),
                    entry.getValue()
                        .copy());
            }
        }
        return copy;
    }

    private static void appendSignature(StringBuilder target, Map<String, MediaWikiTemplateValue> entries) {
        if (entries == null) {
            return;
        }
        for (Map.Entry<String, MediaWikiTemplateValue> entry : entries.entrySet()) {
            target.append(entry.getKey())
                .append('=')
                .append(
                    entry.getValue()
                        .text())
                .append('\u0001');
        }
    }

}
