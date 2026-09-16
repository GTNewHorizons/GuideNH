package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<String> named = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        collect(body, named, positions);
        return new MediaWikiTemplateParameters(List.copyOf(named), List.copyOf(positions));
    }

    private static void collect(List<? extends MdAstAnyContent> nodes, List<String> named, List<Integer> positions) {
        for (MdAstAnyContent node : nodes) {
            if (!(node instanceof MdxJsxElementFields element)) {
                continue;
            }
            if (TemplateTags.isParameter(element.name())) {
                addNamed(element, named);
                addPositional(positionOf(element), positions);
            } else {
                // A <Param> may also be written as an attribute value, where the parser keeps it as raw
                // expression text rather than as a node, so that form is read from the text.
                collectFromAttributeExpressions(element, named, positions);
            }
            // A parameter may sit inside a conditional or any other wrapper, so the walk descends.
            collect(element.children(), named, positions);
        }
    }

    private static void addNamed(MdxJsxElementFields element, List<String> named) {
        addName(attributeText(element, TemplateTags.NAME_ATTRIBUTE), named);
    }

    private static void addName(String name, List<String> named) {
        if (name == null || name.isBlank()) {
            return;
        }
        // Kept exactly as written. A template title is normalized, but a parameter is matched
        // case-insensitively at expansion time, so preserving the declaration keeps completion in step with
        // the template that declares it.
        String trimmed = name.trim();
        if (!named.contains(trimmed)) {
            named.add(trimmed);
        }
    }

    private static void addPositional(Integer position, List<Integer> positions) {
        if (position != null && !positions.contains(position)) {
            positions.add(position);
        }
    }

    private static void collectFromAttributeExpressions(MdxJsxElementFields element, List<String> named,
        List<Integer> positions) {
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

    /** Reads an attribute out of raw tag text, for a {@code <Param>} the parser kept as an expression. */
    private static String attributeText(String tagText, String name) {
        Matcher matcher = ATTRIBUTE_PATTERN_CACHE
            .computeIfAbsent(
                name,
                key -> Pattern.compile("\\b" + Pattern.quote(key) + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')"))
            .matcher(tagText);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private static final Map<String, Pattern> ATTRIBUTE_PATTERN_CACHE = new HashMap<>();

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
