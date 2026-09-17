package com.hfstudio.guidenh.guide.mediawiki.template;

/**
 * A template's identity. Normalization folds the first character to upper case and turns underscores
 * into spaces, which is the only case-insensitivity MediaWiki page titles have: the rest of the name
 * stays case-sensitive, so {@code InfoBox} and {@code Infobox} remain distinct templates.
 */
public record MediaWikiTemplateName(String value) {

    public MediaWikiTemplateName {
        value = normalize(value);
    }

    public static MediaWikiTemplateName parse(String raw) {
        return new MediaWikiTemplateName(raw);
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace('_', ' ')
            .trim();
        StringBuilder builder = new StringBuilder(text.length());
        boolean previousSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                if (previousSpace) {
                    continue;
                }
                previousSpace = true;
            } else {
                previousSpace = false;
            }
            builder.append(ch);
        }
        if (builder.isEmpty()) {
            return "";
        }
        builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        return builder.toString();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    @Override
    public String toString() {
        return value;
    }
}
