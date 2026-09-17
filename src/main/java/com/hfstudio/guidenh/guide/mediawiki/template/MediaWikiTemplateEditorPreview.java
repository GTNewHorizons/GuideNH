package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/** Prepares template source for the in-game editor without treating unresolved parameters as content errors. */
public final class MediaWikiTemplateEditorPreview {

    private static final String DYNAMIC_ATTRIBUTE_MARKER = "<Param";

    private MediaWikiTemplateEditorPreview() {}

    public static List<MdAstAnyContent> prepare(List<? extends MdAstAnyContent> source) {
        List<MdAstAnyContent> prepared = new ArrayList<>(source.size());
        for (MdAstAnyContent node : source) {
            if (node instanceof MdAstYamlFrontmatter) {
                continue;
            }
            if (!(node instanceof MdxJsxElementFields element)) {
                prepared.add(MediaWikiTemplateAst.copy(node));
                continue;
            }
            String tag = element.name();
            if (MediaWikiIncludeControl.NO_INCLUDE.equals(tag) || MediaWikiIncludeControl.INCLUDE_ONLY.equals(tag)
                || MediaWikiIncludeControl.ONLY_INCLUDE.equals(tag)
                || TemplateTags.isConditional(tag)
                || TemplateTags.isStringFunction(tag)
                || TemplateTags.ELSE.equals(tag)
                || TemplateTags.CASE.equals(tag)
                || TemplateTags.DEFAULT.equals(tag)
                || TemplateTags.ARGUMENT.equals(tag)) {
                prepared.addAll(prepare(element.children()));
                continue;
            }
            if (TemplateTags.isParameter(tag)) {
                prepared.addAll(parameterPreview(element));
                continue;
            }
            MdAstAnyContent copy = MediaWikiTemplateAst.copy(node);
            if (copy instanceof MdxJsxElementFields copiedElement) {
                if (hasDynamicAttribute(copiedElement)) {
                    copiedElement.setName(copy instanceof MdxJsxTextElement ? "span" : "div");
                    copiedElement.attributes()
                        .clear();
                    MediaWikiTemplateAst.replaceChildren(copy, List.of(MediaWikiTemplateAst.text("<" + tag + " …>")));
                } else {
                    MediaWikiTemplateAst.replaceChildren(copy, prepare(element.children()));
                }
            }
            prepared.add(copy);
        }
        return prepared;
    }

    private static boolean hasDynamicAttribute(MdxJsxElementFields element) {
        for (MdxJsxAttributeNode attributeNode : element.attributes()) {
            if (attributeNode instanceof MdxJsxAttribute attribute && attribute.hasExpressionValue()
                && attribute.getExpressionValue() != null
                && attribute.getExpressionValue()
                    .contains(DYNAMIC_ATTRIBUTE_MARKER)) {
                return true;
            }
        }
        return false;
    }

    private static String parameterPlaceholder(MdxJsxElementFields element) {
        String fallback = TemplateNodeExpander.attributeValue(element, TemplateTags.DEFAULT_ATTRIBUTE);
        String name = TemplateNodeExpander.attributeValue(element, TemplateTags.NAME_ATTRIBUTE);
        if (name != null && !name.isBlank()) {
            return "{{" + name.trim() + "}}";
        }
        String position = TemplateNodeExpander.attributeValue(element, TemplateTags.POS_ATTRIBUTE);
        return position != null && !position.isBlank() ? "{{" + position.trim() + "}}" : "{{parameter}}";
    }

    private static List<MdAstAnyContent> parameterPreview(MdxJsxElementFields element) {
        String fallback = TemplateNodeExpander.attributeValue(element, TemplateTags.DEFAULT_ATTRIBUTE);
        if (fallback != null) {
            return List.of(MediaWikiTemplateAst.text(fallback));
        }
        if (!element.children()
            .isEmpty()) {
            return prepare(element.children());
        }
        return List.of(MediaWikiTemplateAst.text(parameterPlaceholder(element)));
    }
}
