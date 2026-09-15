package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Ties the dependency graph to recompilation. When a template page changes, the pages that used it are the
 * ones whose compiled output can differ, so that is the set which has to be discarded and queued again.
 *
 * <p>
 * The set is transitive: a page that includes a template which itself includes the edited one is collected
 * too, because the graph records the edges that actually took effect while compiling.
 */
public class MediaWikiTemplateInvalidator {

    @FunctionalInterface
    public interface PageInvalidator {

        void invalidate(ResourceLocation pageId);
    }

    @FunctionalInterface
    public interface PageRecompiler {

        void recompile(ResourceLocation pageId);
    }

    private MediaWikiTemplateInvalidator() {}

    /**
     * Handles a change to the templates held by the given pages, returning every page whose compiled result
     * must be discarded: the changed pages themselves plus the pages that reach them through the graph.
     *
     * @param recompiler queued for each affected page, or null when the caller only wants invalidation
     */
    public static Set<ResourceLocation> onTemplatePagesChanged(List<ResourceLocation> changedPages,
        PageInvalidator invalidator, @Nullable PageRecompiler recompiler) {
        Set<ResourceLocation> affected = collectAffected(changedPages);
        for (ResourceLocation page : affected) {
            invalidator.invalidate(page);
        }
        if (recompiler != null) {
            for (ResourceLocation page : affected) {
                recompiler.recompile(page);
            }
        }
        if (!changedPages.isEmpty()) {
            // The template layer's own logger, which works without a running game unlike the shared one.
            MediaWikiTemplateDiagnostics.reportRebuild(changedPages.size(), affected.size());
        }
        return affected;
    }

    public static Set<ResourceLocation> collectAffected(List<ResourceLocation> changedPages) {
        Set<ResourceLocation> affected = new LinkedHashSet<>(changedPages);
        for (ResourceLocation changed : changedPages) {
            String templateName = MediaWikiTemplatePageIds.templateNameOf(changed);
            if (templateName == null) {
                continue;
            }
            affected.addAll(MediaWikiTemplateDependencyGraph.dependentsOf(MediaWikiTemplateName.parse(templateName)));
        }
        return affected;
    }

    public static boolean isTemplatePage(ResourceLocation pageId) {
        return MediaWikiTemplatePageIds.isTemplatePage(pageId);
    }
}
