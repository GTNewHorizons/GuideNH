package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.List;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/** Produces an independent, transclusion-filtered template body for one call. */
public final class TemplateBodyResolver {

    private TemplateBodyResolver() {}

    public static List<MdAstAnyContent> resolve(PageCompiler compiler, MediaWikiTemplateDefinition definition,
        MediaWikiTemplateContext context) {
        ParsedGuidePage page = compiler.getPageCollection()
            .getParsedPage(definition.pageId());
        if (page == null) {
            context.addIssue(
                MediaWikiTemplateIssueKind.UNKNOWN_TEMPLATE,
                "Template page is missing: " + definition.pageId());
            return List.of();
        }
        if (page.hasParseFailure()) {
            context.addIssue(
                MediaWikiTemplateIssueKind.MALFORMED_INVOCATION,
                "Template page failed to parse: " + definition.pageId());
            return List.of();
        }

        return MediaWikiIncludeControl.copyForTransclusion(
            page.getAstRoot()
                .children());
    }
}
