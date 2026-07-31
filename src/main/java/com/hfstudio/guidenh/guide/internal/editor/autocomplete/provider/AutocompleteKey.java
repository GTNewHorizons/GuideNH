package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.Objects;

import lombok.Getter;

@Getter
public class AutocompleteKey {

    public enum MatchType {
        TAG_NAME,
        ATTR_NAME,
        ATTR_VALUE,
        FENCE_LANGUAGE
    }

    private final MatchType type;
    private final String tagName;
    private final String attrName;

    private AutocompleteKey(MatchType type, String tagName, String attrName) {
        this.type = type;
        this.tagName = tagName;
        this.attrName = attrName;
    }

    public static AutocompleteKey forTag() {
        return new AutocompleteKey(MatchType.TAG_NAME, null, null);
    }

    public static AutocompleteKey forTag(String parentTagName) {
        return new AutocompleteKey(MatchType.TAG_NAME, parentTagName, null);
    }

    public static AutocompleteKey forAttr(String tagName) {
        return new AutocompleteKey(MatchType.ATTR_NAME, Objects.requireNonNull(tagName), null);
    }

    public static AutocompleteKey forValue(String tagName, String attrName) {
        return new AutocompleteKey(
            MatchType.ATTR_VALUE,
            Objects.requireNonNull(tagName),
            Objects.requireNonNull(attrName));
    }

    public static AutocompleteKey forFenceLanguage() {
        return new AutocompleteKey(MatchType.FENCE_LANGUAGE, null, null);
    }

    public boolean matches(MatchType queryType, String queryTag, String queryAttr) {
        if (type != queryType) return false;
        return switch (type) {
            case TAG_NAME, FENCE_LANGUAGE -> true;
            case ATTR_NAME -> tagName.equals("*") || tagName.equals(queryTag);
            case ATTR_VALUE -> (tagName.equals("*") || tagName.equals(queryTag))
                && (attrName.equals("*") || attrName.equals(queryAttr));
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AutocompleteKey that)) return false;
        return type == that.type && Objects.equals(tagName, that.tagName) && Objects.equals(attrName, that.attrName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, tagName, attrName);
    }

    @Override
    public String toString() {
        return "AutocompleteKey{" + type + ", " + tagName + ", " + attrName + "}";
    }
}
