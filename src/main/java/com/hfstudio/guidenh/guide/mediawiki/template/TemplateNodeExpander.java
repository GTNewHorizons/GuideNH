package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

/**
 * Rewrites a template body for one call: {@code <Param>} becomes its value, and a conditional keeps only
 * the branch its test selects. Everything else is left as written, so ordinary tags in a template body are
 * compiled by their own compilers exactly as if the author had written them at the call site.
 */
public class TemplateNodeExpander {

    private TemplateNodeExpander() {}

    public static void expandInto(List<MdAstAnyContent> nodes, MediaWikiTemplateArguments arguments,
        MediaWikiTemplateContext context, PageCompiler compiler) {
        for (int index = 0; index < nodes.size(); index++) {
            MdAstAnyContent node = nodes.get(index);
            if (!(node instanceof MdxJsxElementFields element)) {
                // A parameter written inline sits inside a paragraph or other wrapper, so the walk has to
                // descend through ordinary parents rather than only looking at JSX elements.
                descend(node, arguments, context, compiler);
                continue;
            }
            String tag = element.name();
            if (TemplateTags.isParameter(tag)) {
                replace(nodes, index, resolveParameter(element, arguments, context));
                index--;
                continue;
            }
            if (TemplateTags.isConditional(tag)) {
                List<MdAstAnyContent> chosen = TemplateConditionals.evaluate(element, arguments, context, compiler);
                replace(nodes, index, chosen);
                index--;
                continue;
            }
            if (TemplateTags.isStringFunction(tag)) {
                List<MdAstAnyContent> produced = MediaWikiTemplateFunctions.evaluate(element, arguments, context);
                if (produced != null) {
                    replace(nodes, index, produced);
                    index--;
                    continue;
                }
            }
            if (TemplateTags.TEMPLATE.equals(tag)) {
                List<MdAstAnyContent> included = TemplateInclusion.resolve(element, arguments, context, compiler);
                if (included != null) {
                    replace(nodes, index, included);
                    index--;
                    continue;
                }
            }
            substituteAttributes(element, arguments, context);
            MediaWikiTemplateAst.replaceChildren(node, expanded(element.children(), arguments, context, compiler));
        }
    }

    /**
     * Replaces a {@code <Param>} used as an attribute value. A template writes
     * {@code <ItemImage id={<Param name="icon" />} />}, and the parser hands that back as raw expression text
     * rather than nodes, so the reference is resolved from the text before the tag is compiled.
     */
    private static void substituteAttributes(MdxJsxElementFields element, MediaWikiTemplateArguments arguments,
        MediaWikiTemplateContext context) {
        for (MdxJsxAttributeNode attributeNode : element.attributes()) {
            if (!(attributeNode instanceof MdxJsxAttribute attribute) || !attribute.hasExpressionValue()) {
                continue;
            }
            String resolved = resolveAttributeExpression(attribute.getExpressionValue(), arguments, context);
            if (resolved != null) {
                attribute.setValue(resolved);
            }
        }
    }

    private static @Nullable String resolveAttributeExpression(String expression, MediaWikiTemplateArguments arguments,
        MediaWikiTemplateContext context) {
        if (expression == null || !expression.contains("<Param")) {
            return null;
        }
        String reference = expression.trim();
        if (!reference.startsWith("<Param")) {
            context.addIssue(
                MediaWikiTemplateIssueKind.MALFORMED_INVOCATION,
                "Only a whole <Param> may be used as an attribute value: " + reference);
            return null;
        }
        String name = attributeText(reference, TemplateTags.NAME_ATTRIBUTE);
        String rawPosition = attributeText(reference, "pos");
        String key = name != null && !name.trim()
            .isEmpty() ? MediaWikiTemplateName.normalize(name) : positionalKey(rawPosition, context);
        String fallback = attributeText(reference, TemplateTags.DEFAULT_ATTRIBUTE);
        if (key == null) {
            context.addIssue(
                MediaWikiTemplateIssueKind.MALFORMED_INVOCATION,
                "Attribute parameter needs a name or a positive pos: " + reference);
            return fallback;
        }
        MediaWikiTemplateValue supplied = arguments.get(key);
        if (supplied != null && !supplied.isBlank()) {
            return supplied.text();
        }
        if (fallback != null) {
            return fallback;
        }
        context.addIssue(
            MediaWikiTemplateIssueKind.MISSING_PARAMETER,
            "Attribute parameter " + key + " was not supplied and has no default");
        return null;
    }

    private static @Nullable String positionalKey(@Nullable String rawPosition, MediaWikiTemplateContext context) {
        if (rawPosition == null) {
            return null;
        }
        try {
            int position = Integer.parseInt(rawPosition.trim());
            if (position > 0) {
                return String.valueOf(position);
            }
        } catch (NumberFormatException notANumber) {
            context.addIssue(
                MediaWikiTemplateIssueKind.MALFORMED_INVOCATION,
                "Parameter pos is not a number: " + rawPosition);
            return null;
        }
        context.addIssue(
            MediaWikiTemplateIssueKind.MALFORMED_INVOCATION,
            "Parameter pos must be positive: " + rawPosition);
        return null;
    }

    private static @Nullable String attributeText(String tag, String name) {
        String marker = name + "=\"";
        int start = tag.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + marker.length();
        int valueEnd = tag.indexOf('"', valueStart);
        return valueEnd < 0 ? null : tag.substring(valueStart, valueEnd);
    }

    private static void descend(MdAstAnyContent node, MediaWikiTemplateArguments arguments,
        MediaWikiTemplateContext context, PageCompiler compiler) {
        if (!(node instanceof MdAstParent<?>)) {
            return;
        }
        MediaWikiTemplateAst
            .replaceChildren(node, expanded(MediaWikiTemplateAst.childrenOf(node), arguments, context, compiler));
    }

    private static void replace(List<MdAstAnyContent> nodes, int index, List<MdAstAnyContent> replacement) {
        nodes.remove(index);
        nodes.addAll(index, replacement);
    }

    private static List<MdAstAnyContent> expanded(List<? extends MdAstAnyContent> source,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context, PageCompiler compiler) {
        List<MdAstAnyContent> copy = new ArrayList<>(source);
        expandInto(copy, arguments, context, compiler);
        return copy;
    }

    /**
     * The nodes a {@code <Param>} resolves to. A parameter is identified by {@code name} or by an explicit
     * {@code pos}; an unsupplied one falls back to its {@code default}, then to the text written between the
     * tags, so a template can carry placeholder text of its own.
     */
    private static List<MdAstAnyContent> resolveParameter(MdxJsxElementFields element,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
        String key = parameterKey(element, context);
        if (key == null) {
            return List.of();
        }
        MediaWikiTemplateValue supplied = arguments.get(key);
        if (supplied != null && !supplied.isBlank()) {
            return MediaWikiTemplateValue.copyNodes(supplied.nodes());
        }
        String fallback = attributeValue(element, TemplateTags.DEFAULT_ATTRIBUTE);
        if (fallback != null) {
            return List.of(MediaWikiTemplateAst.text(fallback));
        }
        List<? extends MdAstAnyContent> body = element.children();
        if (!body.isEmpty()) {
            return MediaWikiTemplateValue.copyNodes(body);
        }
        context.addIssue(
            MediaWikiTemplateIssueKind.MISSING_PARAMETER,
            "Parameter " + key + " was not supplied and has no default");
        return List.of();
    }

    static String parameterKey(MdxJsxElementFields element, MediaWikiTemplateContext context) {
        String name = attributeValue(element, TemplateTags.NAME_ATTRIBUTE);
        if (name != null && !name.trim()
            .isEmpty()) {
            return MediaWikiTemplateName.normalize(name);
        }
        String rawPosition = attributeValue(element, "pos");
        if (rawPosition != null) {
            try {
                int position = Integer.parseInt(rawPosition.trim());
                if (position > 0) {
                    return String.valueOf(position);
                }
            } catch (NumberFormatException notANumber) {
                context.addIssue(
                    MediaWikiTemplateIssueKind.MALFORMED_INVOCATION,
                    "Parameter pos is not a number: " + rawPosition);
                return null;
            }
        }
        context.addIssue(
            MediaWikiTemplateIssueKind.MALFORMED_INVOCATION,
            "Parameter needs a name or a positive pos attribute");
        return null;
    }

    static String attributeValue(MdxJsxElementFields element, String name) {
        MdxJsxAttribute attribute = element.getAttribute(name);
        if (attribute == null) {
            return null;
        }
        if (attribute.hasStringValue()) {
            return attribute.getStringValue();
        }
        return attribute.hasExpressionValue() ? attribute.getExpressionValue() : null;
    }

    static String bodyText(MdxJsxElementFields element) {
        StringBuilder builder = new StringBuilder();
        for (MdAstAnyContent child : element.children()) {
            builder.append(MediaWikiTemplateAst.flatten(child));
        }
        return builder.toString();
    }
}
