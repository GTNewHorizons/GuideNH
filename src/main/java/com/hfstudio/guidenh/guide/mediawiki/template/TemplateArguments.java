package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * Reads the arguments of a {@code <Template>} call, wherever the call appears. A child
 * {@code <Arg name="x">v</Arg>} binds {@code x} and one without a name binds the next positional slot; an
 * attribute is shorthand for the same thing.
 */
public class TemplateArguments {

    private TemplateArguments() {}

    public static MediaWikiTemplateArguments collect(MdxJsxElementFields element) {
        return collect(element, null);
    }

    public static MediaWikiTemplateArguments collect(MdxJsxElementFields element,
        @Nullable MediaWikiTemplateArguments outer) {
        MediaWikiTemplateArguments arguments = new MediaWikiTemplateArguments();
        int position = 0;
        boolean nameAttributeSeen = false;
        for (MdAstAnyContent child : element.children()) {
            if (child instanceof MdxJsxElementFields direct && TemplateTags.isArgument(direct.name())) {
                position++;
                put(
                    arguments,
                    attributeOf(direct, TemplateTags.NAME_ATTRIBUTE),
                    position,
                    valueOf(direct.children(), outer));
            } else if (child instanceof MdxJsxElementFields wrapper && TemplateTags.isArgumentWrapper(wrapper.name())) {
                for (MdAstAnyContent nested : wrapper.children()) {
                    if (nested instanceof MdxJsxElementFields arg && TemplateTags.isArgument(arg.name())) {
                        position++;
                        put(
                            arguments,
                            attributeOf(arg, TemplateTags.NAME_ATTRIBUTE),
                            position,
                            valueOf(arg.children(), outer));
                    }
                }
            }
        }
        for (MdxJsxAttributeNode attributeNode : element.attributes()) {
            if (!(attributeNode instanceof MdxJsxAttribute attribute)) {
                continue;
            }
            if (TemplateTags.NAME_ATTRIBUTE.equals(attribute.name)) {
                if (!nameAttributeSeen) {
                    nameAttributeSeen = true;
                    continue;
                }
            } else if (!TemplateTags.isArgumentAttribute(attribute.name)) {
                continue;
            }
            position++;
            String raw = attributeValue(attribute);
            MediaWikiTemplateValue forwarded = outer == null ? null : outer.get(raw);
            put(
                arguments,
                attribute.name,
                position,
                forwarded != null ? forwarded.copy() : MediaWikiTemplateValue.ofText(raw));
        }
        return arguments;
    }

    private static MediaWikiTemplateValue valueOf(List<? extends MdAstAnyContent> nodes,
        @Nullable MediaWikiTemplateArguments outer) {
        if (outer == null) {
            return MediaWikiTemplateValue.ofNodes(nodes);
        }
        List<MdAstAnyContent> resolved = new ArrayList<>(nodes.size());
        for (MdAstAnyContent node : nodes) {
            if (node instanceof MdxJsxElementFields nested && TemplateTags.isParameter(nested.name())) {
                String key = attributeOf(nested, TemplateTags.NAME_ATTRIBUTE);
                MediaWikiTemplateValue outerValue = key == null ? null : outer.get(key);
                if (outerValue != null) {
                    resolved.addAll(MediaWikiTemplateValue.copyNodes(outerValue.nodes()));
                    continue;
                }
                String fallback = attributeOf(nested, TemplateTags.DEFAULT_ATTRIBUTE);
                if (fallback != null) {
                    resolved.add(MediaWikiTemplateAst.text(fallback));
                    continue;
                }
                resolved.addAll(MediaWikiTemplateValue.copyNodes(nested.children()));
                continue;
            }
            resolved.add(MediaWikiTemplateAst.copy(node));
        }
        return MediaWikiTemplateValue.ofNodes(resolved);
    }

    private static void put(MediaWikiTemplateArguments arguments, @Nullable String paramName, int position,
        MediaWikiTemplateValue value) {
        if (paramName == null || paramName.trim()
            .isEmpty()) {
            arguments.putPositional(position, value);
        } else {
            arguments.putNamed(paramName, value);
        }
    }

    public static @Nullable String attributeOf(MdxJsxElementFields element, String name) {
        MdxJsxAttribute attribute = element.getAttribute(name);
        return attribute == null ? null : attributeValue(attribute);
    }

    public static String attributeValue(MdxJsxAttribute attribute) {
        if (attribute.hasStringValue()) {
            return attribute.getStringValue();
        }
        return attribute.hasExpressionValue() ? attribute.getExpressionValue() : "";
    }

    public static List<String> argumentTexts(MdxJsxElementFields element) {
        List<String> texts = new ArrayList<>();
        for (MdAstAnyContent child : element.children()) {
            if (child instanceof MdxJsxElementFields arg && TemplateTags.isArgument(arg.name())) {
                String text = MediaWikiTemplateValue.ofNodes(arg.children())
                    .text();
                if (!text.trim()
                    .isEmpty()) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }
}
