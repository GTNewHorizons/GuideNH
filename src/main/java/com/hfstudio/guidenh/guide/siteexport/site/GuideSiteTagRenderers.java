package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Asks the contributed {@link GuideSiteTagRenderer}s for an element, so a tag of another mod is exported by
 * its own renderer instead of being dropped. The first renderer that answers wins, and the built-in ones
 * run only when no contributor claims the tag.
 */
public class GuideSiteTagRenderers {

    private GuideSiteTagRenderers() {}

    /** The renderers that apply to a guide: the guide's own first, then the globally registered ones. */
    public static List<GuideSiteTagRenderer> of(@Nullable Guide guide) {
        List<GuideSiteTagRenderer> declared = guide != null ? guide.getExtensions()
            .get(GuideSiteTagRenderer.EXTENSION_POINT) : List.of();
        List<GuideSiteTagRenderer> global = GuideNhIntegrationRegistry.global()
            .siteTagRenderers();
        if (global.isEmpty()) {
            return declared;
        }
        if (declared.isEmpty()) {
            return global;
        }
        List<GuideSiteTagRenderer> all = new ArrayList<>(declared.size() + global.size());
        all.addAll(declared);
        all.addAll(global);
        return all;
    }

    /**
     * The markup a contributed renderer produces for an element.
     *
     * @return the rendered markup, or null when no renderer answers
     */
    @Nullable
    public static String render(List<GuideSiteTagRenderer> renderers, GuideSiteTagRenderContext context,
        MdxJsxElementFields element) {
        String tagName = element.name();
        if (tagName == null || renderers.isEmpty()) {
            return null;
        }
        for (GuideSiteTagRenderer renderer : renderers) {
            if (!answers(renderer, tagName)) {
                continue;
            }
            try {
                String markup = renderer.render(context, element);
                if (markup != null) {
                    return markup;
                }
            } catch (RuntimeException e) {
                // A renderer of another mod must not break the export of a page it has nothing to do with,
                // so a failed one is reported and the next renderer, or the built-in one, gets its turn.
                GuideDebugLog.error(
                    "[GuideNH] [GuideSiteTagRenderer] {} failed to render <{}>: {}",
                    renderer.getClass()
                        .getSimpleName(),
                    tagName,
                    e.toString());
            }
        }
        return null;
    }

    private static boolean answers(GuideSiteTagRenderer renderer, String tagName) {
        try {
            Set<String> tagNames = renderer.getTagNames();
            return tagNames != null && tagNames.contains(tagName);
        } catch (RuntimeException e) {
            GuideDebugLog.error(
                "[GuideNH] [GuideSiteTagRenderer] {} failed to publish its tags: {}",
                renderer.getClass()
                    .getSimpleName(),
                e.toString());
            return false;
        }
    }
}
