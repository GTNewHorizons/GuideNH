package com.hfstudio.guidenh.guide.compiler.tags.mediawiki;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateArguments;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateContext;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateDefinition;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateDiagnostics;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateIssueKind;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateRepository;
import com.hfstudio.guidenh.guide.mediawiki.template.TemplateArguments;
import com.hfstudio.guidenh.guide.mediawiki.template.TemplateBodyResolver;
import com.hfstudio.guidenh.guide.mediawiki.template.TemplateNodeExpander;
import com.hfstudio.guidenh.guide.mediawiki.template.TemplateTags;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * Transcludes an MDX template at compile time. The body is read from the template page during compilation,
 * so nothing is resolved while the guide is being viewed and no page text is rewritten before parsing.
 */
public class TemplateCompiler extends BlockTagCompiler {

    public static final String TAG_NAME = TemplateTags.TEMPLATE;
    private static final int MAX_INCLUSION_DEPTH = MediaWikiTemplateContext.MAX_DEPTH;

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton(TAG_NAME);
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String name = MdxAttrs.getString(compiler, parent, el, TemplateTags.NAME_ATTRIBUTE, null);
        if (name == null || name.trim()
            .isEmpty()) {
            parent.appendError(compiler, "Template requires a non-empty name attribute.", el);
            return;
        }

        MediaWikiTemplateDefinition definition = MediaWikiTemplateRepository.findByName(name.trim());
        MediaWikiTemplateContext context = MediaWikiTemplateContext.forPage(compiler);
        if (definition == null) {
            context.addIssue(MediaWikiTemplateIssueKind.UNKNOWN_TEMPLATE, "Unknown template " + name.trim());
            context.report();
            parent.appendError(compiler, "Unknown template: " + name.trim(), el);
            return;
        }
        // Recording here rather than after the body resolves is deliberate: a page depends on a template
        // even if the body turns out to be empty, because the next edit to that template must reach it.
        compiler.recordTemplateDependency(definition.name());
        if (!context.enter()) {
            context.addIssue(
                MediaWikiTemplateIssueKind.RECURSION_LIMIT,
                "Template nesting is deeper than " + MAX_INCLUSION_DEPTH);
            context.report();
            parent.appendError(compiler, "Template nesting is too deep: " + name.trim(), el);
            return;
        }

        try {
            MediaWikiTemplateArguments arguments = TemplateArguments.collect(el);
            if (!context.markVisited(definition, arguments)) {
                context.addIssue(
                    MediaWikiTemplateIssueKind.RECURSION_LIMIT,
                    "Template " + name.trim() + " includes itself");
                context.report();
                parent.appendError(compiler, "Template includes itself: " + name.trim(), el);
                return;
            }
            try {
                List<MdAstAnyContent> body = TemplateBodyResolver.resolve(compiler, definition, arguments, context);
                if (body.isEmpty()) {
                    return;
                }
                context.beginTemplatePage(definition.pageId());
                try {
                    TemplateNodeExpander.expandInto(body, arguments, context, compiler);
                } finally {
                    context.endTemplatePage();
                }
                compiler.compileBlockContext(body, parent);
            } finally {
                context.unmarkVisited(definition, arguments);
            }
        } catch (RuntimeException | StackOverflowError failed) {
            // A template must not be able to break the page that calls it, so an unexpected failure is
            // reported where the call is and the rest of the page still compiles.
            MediaWikiTemplateDiagnostics.reportFailure(compiler.getPageId(), failed);
            parent.appendError(compiler, "Template failed: " + name.trim(), el);
        } finally {
            context.leave();
            context.report();
        }
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        for (String text : TemplateArguments.argumentTexts(el)) {
            sink.appendText(el, text);
            sink.appendBreak();
        }
    }
}
