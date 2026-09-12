package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

/**
 * Asks the contributed {@link CodeFenceRenderer}s what a fence body means, so a fence name a mod declares is.
 */
public class CodeFenceRenderers {

    private CodeFenceRenderers() {}

    public static List<CodeFenceRenderer> of(@Nullable Guide guide) {
        List<CodeFenceRenderer> declared = guide != null ? guide.getExtensions()
            .get(CodeFenceRenderer.EXTENSION_POINT) : List.of();
        List<CodeFenceRenderer> global = GuideNhIntegrationRegistry.global()
            .codeFenceRenderers();
        if (global.isEmpty()) {
            return declared;
        }
        if (declared.isEmpty()) {
            return global;
        }
        List<CodeFenceRenderer> all = new ArrayList<>(declared.size() + global.size());
        all.addAll(declared);
        all.addAll(global);
        return all;
    }

    /** The block a contributed renderer produces for a fence. */
    @Nullable
    public static LytBlock render(List<CodeFenceRenderer> renderers, PageCompiler compiler, @Nullable String fenceName,
        String codeText, @Nullable String meta) {
        if (fenceName == null || fenceName.isEmpty() || renderers.isEmpty()) {
            return null;
        }
        for (CodeFenceRenderer renderer : renderers) {
            if (!answers(renderer, fenceName)) {
                continue;
            }
            try {
                LytBlock block = renderer.renderFence(compiler, fenceName, codeText, meta);
                if (block != null) {
                    return block;
                }
            } catch (RuntimeException e) {
                GuideDebugLog.error(
                    "[GuideNH] [CodeFenceRenderer] {} failed to render the '{}' fence: {}",
                    renderer.getClass()
                        .getSimpleName(),
                    fenceName,
                    e.toString());
            }
        }
        return null;
    }

    private static boolean answers(CodeFenceRenderer renderer, String fenceName) {
        try {
            Set<String> names = renderer.getFenceNames();
            return names != null && names.contains(fenceName);
        } catch (RuntimeException e) {
            GuideDebugLog.error(
                "[GuideNH] [CodeFenceRenderer] {} failed to publish its fence names: {}",
                renderer.getClass()
                    .getSimpleName(),
                e.toString());
            return false;
        }
    }

    /**
     * Whether any contributed renderer owns a fence name.
     *
     * <p>
     * The export asks this before deciding to treat a fence as a contributed one, and a renderer that fails
     * is reported and skipped here exactly as it is when rendering, so a broken plugin cannot fail a page.
     */
    public static boolean owns(List<CodeFenceRenderer> renderers, @Nullable String fenceName) {
        if (fenceName == null || fenceName.isEmpty()) {
            return false;
        }
        for (CodeFenceRenderer renderer : renderers) {
            if (answers(renderer, fenceName)) {
                return true;
            }
        }
        return false;
    }

    /** The markup a contributed renderer produces for a fence on the exported site. */
    @Nullable
    public static String renderSite(List<CodeFenceRenderer> renderers, @Nullable String fenceName, String codeText,
        @Nullable String meta) {
        if (fenceName == null || fenceName.isEmpty()) {
            return null;
        }
        for (CodeFenceRenderer renderer : renderers) {
            if (!answers(renderer, fenceName)) {
                continue;
            }
            try {
                String markup = renderer.renderSiteFence(fenceName, codeText, meta);
                if (markup != null) {
                    return markup;
                }
            } catch (RuntimeException e) {
                GuideDebugLog.error(
                    "[GuideNH] [CodeFenceRenderer] {} failed to render the '{}' fence for the site: {}",
                    renderer.getClass()
                        .getSimpleName(),
                    fenceName,
                    e.toString());
            }
        }
        return null;
    }
}
