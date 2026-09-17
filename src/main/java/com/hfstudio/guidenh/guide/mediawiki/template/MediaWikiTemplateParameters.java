package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * The parameters a template declares, read from its body once when the repository is rebuilt. The editor
 * uses this to complete a call's arguments, and nothing here affects compilation: a template still resolves
 * whatever the call supplies, whether or not it appears in this list.
 */
public record MediaWikiTemplateParameters(List<String> named, List<Integer> positions) {

    public static final MediaWikiTemplateParameters EMPTY = new MediaWikiTemplateParameters(List.of(), List.of());

    public static MediaWikiTemplateParameters of(@Nullable List<? extends MdAstAnyContent> body) {
        if (body == null || body.isEmpty()) {
            return EMPTY;
        }
        Set<String> named = new LinkedHashSet<>();
        Set<Integer> positions = new LinkedHashSet<>();
        collect(body, named, positions);
        return new MediaWikiTemplateParameters(List.copyOf(named), List.copyOf(positions));
    }

    private static void collect(List<? extends MdAstAnyContent> nodes, Collection<String> named,
        Collection<Integer> positions) {
        for (MdAstAnyContent node : nodes) {
            if (!(node instanceof MdxJsxElementFields element)) {
                continue;
            }
            if (TemplateTags.isParameter(element.name())) {
                addNamed(element, named);
                addPositional(positionOf(element), positions);
            } else {
                collectFromAttributeExpressions(element, named, positions);
            }
            collect(element.children(), named, positions);
        }
    }

    private static void addNamed(MdxJsxElementFields element, Collection<String> named) {
        addName(attributeText(element, TemplateTags.NAME_ATTRIBUTE), named);
    }

    private static void addName(String name, Collection<String> named) {
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        named.add(trimmed);
    }

    private static void addPositional(Integer position, Collection<Integer> positions) {
        if (position != null) {
            positions.add(position);
        }
    }

    private static void collectFromAttributeExpressions(MdxJsxElementFields element, Collection<String> named,
        Collection<Integer> positions) {
        for (MdxJsxAttributeNode attributeNode : element.attributes()) {
            if (!(attributeNode instanceof MdxJsxAttribute attribute) || !attribute.hasExpressionValue()) {
                continue;
            }
            String expression = attribute.getExpressionValue();
            if (expression == null || !expression.contains("<Param")) {
                continue;
            }
            String name = attributeText(expression, TemplateTags.NAME_ATTRIBUTE);
            if (name != null && !name.isBlank()) {
                addName(name, named);
            } else {
                addPositional(positionFrom(attributeText(expression, TemplateTags.POS_ATTRIBUTE)), positions);
            }
        }
    }

    private static Integer positionOf(MdxJsxElementFields element) {
        return positionFrom(attributeText(element, TemplateTags.POS_ATTRIBUTE));
    }

    private static Integer positionFrom(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            int position = Integer.parseInt(raw.trim());
            return position > 0 ? position : null;
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private static String attributeText(MdxJsxElementFields element, String name) {
        MdxJsxAttribute attribute = element.getAttribute(name);
        if (attribute == null) {
            return null;
        }
        if (attribute.hasStringValue()) {
            return attribute.getStringValue();
        }
        return attribute.hasExpressionValue() ? attribute.getExpressionValue() : null;
    }

    private static String attributeText(String tagText, String name) {
        Pattern pattern = TemplateTags.NAME_ATTRIBUTE.equals(name) ? NAME_ATTRIBUTE_PATTERN : POS_ATTRIBUTE_PATTERN;
        Matcher matcher = pattern.matcher(tagText);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private static Pattern attributePattern(String name) {
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    }

    private static final Pattern NAME_ATTRIBUTE_PATTERN = attributePattern(TemplateTags.NAME_ATTRIBUTE);
    private static final Pattern POS_ATTRIBUTE_PATTERN = attributePattern(TemplateTags.POS_ATTRIBUTE);

    public boolean hasNamed() {
        return !named.isEmpty();
    }

    public int highestPosition() {
        int highest = 0;
        for (int position : positions) {
            highest = Math.max(highest, position);
        }
        return highest;
    }
}
