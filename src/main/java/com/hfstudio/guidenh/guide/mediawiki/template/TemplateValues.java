package com.hfstudio.guidenh.guide.mediawiki.template;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Reads a value a tag needs. A value may name a parameter, be written literally, be given by a nested
 * {@code <Param>}, or be left as the tag's body, so these readers cover each of those forms.
 */
public class TemplateValues {

    private TemplateValues() {}

    /**
     * Reads a value that must name a parameter, which is what a presence test needs: {@code <If test="tier">}
     * asks whether the argument {@code tier} was supplied, so an absent one is false rather than the literal
     * text {@code tier}.
     */
    public static @Nullable String readParameterName(MdxJsxElementFields element, String attributeName,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        String nested = readNestedParameter(element, arguments, context);
        if (nested != null) {
            return nested;
        }
        String name = TemplateNodeExpander.attributeValue(element, attributeName);
        if (name == null || name.isEmpty()) {
            return null;
        }
        MediaWikiTemplateValue supplied = arguments.get(name);
        return supplied != null ? supplied.text() : null;
    }

    /**
     * Reads a value that may name a parameter or be literal text. A comparison needs this because templates
     * write {@code <IfEq a="tier" b="mv">}: the first names an argument and the second is the text to match.
     */
    public static @Nullable String readComparable(MdxJsxElementFields element, String attributeName,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        String nested = readNestedParameter(element, arguments, context);
        if (nested != null) {
            return nested;
        }
        String name = TemplateNodeExpander.attributeValue(element, attributeName);
        if (name == null || name.isEmpty()) {
            return null;
        }
        MediaWikiTemplateValue supplied = arguments.get(name);
        return supplied != null ? supplied.text() : name;
    }

    public static @Nullable String read(MdxJsxElementFields element, String attributeName,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        String nested = readNestedParameter(element, arguments, context);
        if (nested != null) {
            return nested;
        }
        String direct = TemplateNodeExpander.attributeValue(element, attributeName);
        if (direct != null && !direct.isEmpty()) {
            MediaWikiTemplateValue supplied = arguments.get(direct);
            return supplied != null ? supplied.text() : direct;
        }
        String body = TemplateNodeExpander.bodyText(element);
        return body.isEmpty() ? null : body;
    }

    private static @Nullable String readNestedParameter(MdxJsxElementFields element,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        for (var child : element.children()) {
            if (child instanceof MdxJsxElementFields nested && TemplateTags.isParameter(nested.name())) {
                String key = TemplateNodeExpander.parameterKey(nested, context);
                if (key == null) {
                    return null;
                }
                MediaWikiTemplateValue value = arguments.get(key);
                if (value != null) {
                    return value.text();
                }
                String fallback = TemplateNodeExpander.attributeValue(nested, TemplateTags.DEFAULT_ATTRIBUTE);
                return fallback != null ? fallback : TemplateNodeExpander.bodyText(nested);
            }
        }
        return null;
    }

    public static boolean equalTo(@Nullable String left, @Nullable String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        Double numericA = parseNumeric(a);
        Double numericB = parseNumeric(b);
        if (numericA != null && numericB != null) {
            return numericA.doubleValue() == numericB.doubleValue();
        }
        return a.equals(b);
    }

    public static @Nullable Double parseNumeric(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException notNumeric) {
            return null;
        }
    }

    public static int toInt(@Nullable String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException notNumeric) {
            return fallback;
        }
    }
}
