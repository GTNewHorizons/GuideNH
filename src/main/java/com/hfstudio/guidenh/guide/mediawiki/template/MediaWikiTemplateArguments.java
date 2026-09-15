package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * The arguments of one {@code <Template>} call. Positional arguments are numbered from 1 in the order
 * written; named arguments are keyed by their normalized name.
 */
public class MediaWikiTemplateArguments {

    private final List<String> positionalKeys = new ArrayList<>();
    private final List<MediaWikiTemplateValue> positionalValues = new ArrayList<>();
    private final List<String> namedKeys = new ArrayList<>();
    private final List<MediaWikiTemplateValue> namedValues = new ArrayList<>();

    public void putPositional(int position, MediaWikiTemplateValue value) {
        positionalKeys.add(String.valueOf(position));
        positionalValues.add(value);
    }

    public void putNamed(String name, MediaWikiTemplateValue value) {
        namedKeys.add(MediaWikiTemplateName.normalize(name));
        namedValues.add(value);
    }

    public @Nullable MediaWikiTemplateValue get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        int positionalIndex = positionalKeys.indexOf(key);
        if (positionalIndex >= 0) {
            return positionalValues.get(positionalIndex);
        }
        // Named keys are stored normalized, so a lookup has to be normalized the same way or a parameter
        // whose declaration differs only in case would never resolve.
        int namedIndex = namedKeys.indexOf(MediaWikiTemplateName.normalize(key));
        return namedIndex >= 0 ? namedValues.get(namedIndex) : null;
    }

    public boolean isEmpty() {
        return positionalValues.isEmpty() && namedValues.isEmpty();
    }

    public List<String> namedKeys() {
        return List.copyOf(namedKeys);
    }

    public String signature() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < positionalValues.size(); i++) {
            builder.append(positionalKeys.get(i))
                .append('=')
                .append(
                    positionalValues.get(i)
                        .text())
                .append('\u0001');
        }
        for (int i = 0; i < namedValues.size(); i++) {
            builder.append(namedKeys.get(i))
                .append('=')
                .append(
                    namedValues.get(i)
                        .text())
                .append('\u0001');
        }
        return builder.toString();
    }

    public MediaWikiTemplateArguments copy() {
        MediaWikiTemplateArguments copy = new MediaWikiTemplateArguments();
        for (int i = 0; i < positionalValues.size(); i++) {
            copy.putPositional(
                Integer.parseInt(positionalKeys.get(i)),
                positionalValues.get(i)
                    .copy());
        }
        for (int i = 0; i < namedValues.size(); i++) {
            copy.putNamed(
                namedKeys.get(i),
                namedValues.get(i)
                    .copy());
        }
        return copy;
    }
}
