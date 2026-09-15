package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;

/**
 * Per-compile state: the recursion guard, the inclusion budget, and the issues to report once the call has
 * finished. One context covers a whole page compile, so a page calling many templates is bounded as a whole.
 */
public class MediaWikiTemplateContext {

    public static final int MAX_DEPTH = 32;
    public static final int MAX_INCLUSIONS = 4096;
    /** Only so many problems are kept per page; the rest are counted rather than stored. */
    private static final int MAX_RECORDED_ISSUES = 64;

    private int suppressedIssues;

    private final String sourcePack;
    private final String language;
    private final ResourceLocation pageId;
    private final List<MediaWikiTemplateIssue> issues = new ArrayList<>();
    private final Set<String> visited = new HashSet<>();
    // Innermost last; the top is the template whose body is being expanded right now.
    private final List<ResourceLocation> expandingTemplatePages = new ArrayList<>();

    private int depth;
    private int inclusions;

    public MediaWikiTemplateContext(String sourcePack, String language, ResourceLocation pageId) {
        this.sourcePack = sourcePack;
        this.language = language;
        this.pageId = pageId;
    }

    /**
     * The context for the page being compiled. One instance is shared by every call on that page, so the
     * inclusion budget applies to the page as a whole rather than being reset for each call.
     */
    public static MediaWikiTemplateContext forPage(PageCompiler compiler) {
        return compiler.templateContext();
    }

    public String sourcePack() {
        return sourcePack;
    }

    public String language() {
        return language;
    }

    public ResourceLocation pageId() {
        return pageId;
    }

    public int depth() {
        return depth;
    }

    public boolean enter() {
        if (depth >= MAX_DEPTH) {
            return false;
        }
        depth++;
        return true;
    }

    public void leave() {
        if (depth > 0) {
            depth--;
        }
    }

    public boolean markVisited(MediaWikiTemplateDefinition definition, MediaWikiTemplateArguments arguments) {
        if (inclusions >= MAX_INCLUSIONS) {
            return false;
        }
        inclusions++;
        return visited.add(keyOf(definition, arguments));
    }

    public void unmarkVisited(MediaWikiTemplateDefinition definition, MediaWikiTemplateArguments arguments) {
        visited.remove(keyOf(definition, arguments));
    }

    private static String keyOf(MediaWikiTemplateDefinition definition, MediaWikiTemplateArguments arguments) {
        return definition.name()
            .value() + '\u0000'
            + arguments.signature();
    }

    public void addIssue(MediaWikiTemplateIssueKind kind, String message) {
        // Bounded because a page that fails the same way thousands of times would otherwise accumulate an
        // issue per failure; the count is kept so the log can still say how many there were.
        if (issues.size() < MAX_RECORDED_ISSUES) {
            issues.add(new MediaWikiTemplateIssue(kind, message));
        } else {
            suppressedIssues++;
        }
    }

    public void recordInclusion(MediaWikiTemplateName included) {
        ResourceLocation owner = expandingTemplatePages.isEmpty() ? null
            : expandingTemplatePages.get(expandingTemplatePages.size() - 1);
        if (owner != null) {
            MediaWikiTemplateDependencyGraph.addEdge(owner, included);
        }
    }

    public void beginTemplatePage(ResourceLocation templatePageId) {
        expandingTemplatePages.add(templatePageId);
    }

    public void endTemplatePage() {
        if (!expandingTemplatePages.isEmpty()) {
            expandingTemplatePages.remove(expandingTemplatePages.size() - 1);
        }
    }

    public List<MediaWikiTemplateIssue> issues() {
        return List.copyOf(issues);
    }

    public void report() {
        MediaWikiTemplateDiagnostics.report(pageId, issues);
    }
}
