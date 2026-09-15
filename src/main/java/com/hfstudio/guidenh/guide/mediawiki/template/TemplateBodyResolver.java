package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * Produces a template's body for one call by reading the template page's own AST and copying it. Reading
 * the page is what makes language fallback and caching apply for free: by the time the compiler runs, the
 * page collection already resolved which file the active language should use.
 */
public class TemplateBodyResolver {

    private TemplateBodyResolver() {}

    public static List<MdAstAnyContent> resolve(PageCompiler compiler, MediaWikiTemplateDefinition definition,
        MediaWikiTemplateArguments arguments, MediaWikiTemplateContext context) {
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

        List<MdAstAnyContent> body = new ArrayList<>();
        for (MdAstAnyContent child : page.getAstRoot()
            .children()) {
            // Frontmatter is metadata rather than content, so transcluding it would inject a YAML block
            // into the middle of the caller's page where it would render as text.
            if (child instanceof MdAstYamlFrontmatter) {
                continue;
            }
            body.add(MediaWikiTemplateAst.copy(child));
        }
        return MediaWikiIncludeControl.filterForTransclusion(body);
    }
}
