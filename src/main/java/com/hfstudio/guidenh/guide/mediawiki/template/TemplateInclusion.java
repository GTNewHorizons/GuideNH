package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * Expands a {@code <Template>} that appears inside another template's body. The outer call's arguments are
 * visible to the inner call's arguments, so a wrapper can forward a value it was given.
 */
public class TemplateInclusion {

    private TemplateInclusion() {}

    public static @Nullable List<MdAstAnyContent> resolve(MdxJsxElementFields element,
        MediaWikiTemplateArguments outerArguments, MediaWikiTemplateContext context, PageCompiler compiler) {
        String name = TemplateNodeExpander.attributeValue(element, TemplateTags.NAME_ATTRIBUTE);
        if (name == null || name.trim()
            .isEmpty()) {
            context.addIssue(MediaWikiTemplateIssueKind.MALFORMED_INVOCATION, "Template needs a name attribute");
            return null;
        }
        MediaWikiTemplateDefinition definition = MediaWikiTemplateRepository.findByName(name.trim());
        if (definition == null) {
            context.addIssue(MediaWikiTemplateIssueKind.UNKNOWN_TEMPLATE, "Unknown template " + name.trim());
            return null;
        }
        if (!context.enter()) {
            context.addIssue(
                MediaWikiTemplateIssueKind.RECURSION_LIMIT,
                "Template nesting is deeper than " + MediaWikiTemplateContext.MAX_DEPTH);
            return null;
        }
        try {
            MediaWikiTemplateArguments arguments = TemplateArguments.collect(element, outerArguments);
            context.recordInclusion(definition.name());
            String inclusionKey = context.enterInclusion(definition, arguments);
            if (inclusionKey == null) {
                context.addIssue(
                    MediaWikiTemplateIssueKind.RECURSION_LIMIT,
                    "Template " + name.trim() + " includes itself");
                return null;
            }
            try {
                List<MdAstAnyContent> body = TemplateBodyResolver.resolve(compiler, definition, context);
                if (body.isEmpty()) {
                    return body;
                }
                context.beginTemplatePage(definition.pageId());
                try {
                    TemplateNodeExpander.expandInto(body, arguments, context, compiler);
                } finally {
                    context.endTemplatePage();
                }
                return isInlineCall(element) ? unwrapParagraphs(body) : body;
            } finally {
                context.leaveInclusion(inclusionKey);
            }
        } finally {
            context.leave();
        }
    }

    private static boolean isInlineCall(MdxJsxElementFields element) {
        return element instanceof MdxJsxTextElement;
    }

    private static List<MdAstAnyContent> unwrapParagraphs(List<MdAstAnyContent> body) {
        List<MdAstAnyContent> unwrapped = new ArrayList<>(body.size());
        for (MdAstAnyContent node : body) {
            if (node instanceof MdxJsxElementFields element && "p".equals(element.name())) {
                unwrapped.addAll(MediaWikiTemplateValue.copyNodes(element.children()));
            } else {
                unwrapped.add(node);
            }
        }
        return unwrapped;
    }
}
