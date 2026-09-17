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
    private static final int MAX_RECORDED_ISSUES = 64;

    private final String sourcePack;
    private final String language;
    private final ResourceLocation pageId;
    private List<MediaWikiTemplateIssue> issues;
    private String onlyVisited;
    private Set<String> visited;
    private ResourceLocation expandingTemplatePage;
    private List<ResourceLocation> parentTemplatePages;

    private int depth;
    private int inclusions;

    public MediaWikiTemplateContext(String sourcePack, String language, ResourceLocation pageId) {
        this.sourcePack = sourcePack;
        this.language = language;
        this.pageId = pageId;
    }

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

    public String enterInclusion(MediaWikiTemplateDefinition definition, MediaWikiTemplateArguments arguments) {
        if (inclusions >= MAX_INCLUSIONS) {
            return null;
        }
        inclusions++;
        String key = keyOf(definition, arguments);
        if (onlyVisited == null && visited == null) {
            onlyVisited = key;
            return key;
        }
        if (visited == null) {
            visited = new HashSet<>();
            visited.add(onlyVisited);
            onlyVisited = null;
        }
        return visited.add(key) ? key : null;
    }

    public void leaveInclusion(String key) {
        if (key == null) {
            return;
        }
        if (visited != null) {
            visited.remove(key);
            if (visited.isEmpty()) {
                visited = null;
            }
        } else if (key.equals(onlyVisited)) {
            onlyVisited = null;
        }
    }

    private static String keyOf(MediaWikiTemplateDefinition definition, MediaWikiTemplateArguments arguments) {
        return definition.name()
            .value() + '\u0000'
            + arguments.signature();
    }

    public void addIssue(MediaWikiTemplateIssueKind kind, String message) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        if (issues.size() < MAX_RECORDED_ISSUES) {
            issues.add(new MediaWikiTemplateIssue(kind, message));
        }
    }

    public void recordInclusion(MediaWikiTemplateName included) {
        if (expandingTemplatePage != null) {
            MediaWikiTemplateDependencyGraph.addEdge(expandingTemplatePage, included);
        }
    }

    public void beginTemplatePage(ResourceLocation templatePageId) {
        if (expandingTemplatePage != null) {
            if (parentTemplatePages == null) {
                parentTemplatePages = new ArrayList<>();
            }
            parentTemplatePages.add(expandingTemplatePage);
        }
        expandingTemplatePage = templatePageId;
    }

    public void endTemplatePage() {
        expandingTemplatePage = parentTemplatePages == null || parentTemplatePages.isEmpty() ? null
            : parentTemplatePages.remove(parentTemplatePages.size() - 1);
        if (parentTemplatePages != null && parentTemplatePages.isEmpty()) {
            parentTemplatePages = null;
        }
    }

    public List<MediaWikiTemplateIssue> issues() {
        return issues == null ? List.of() : List.copyOf(issues);
    }

    public void report() {
        if (issues != null) {
            MediaWikiTemplateDiagnostics.report(pageId, issues);
        }
    }
}
