package com.hfstudio.guidenh.guide.mediawiki.template;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Answers {@code <IfExist>}. Existence is checked against the page collection the compiler already holds, so
 * a template sees the pages the current language and pack resolved to rather than the global guide registry,
 * which is not populated yet while a guide is being built.
 */
public class MediaWikiPageExistence {

    private MediaWikiPageExistence() {}

    public static boolean pageExists(String reference, MediaWikiTemplateContext context,
        @Nullable PageCollectionLookup pages) {
        ResourceLocation resolved = resolve(reference, context);
        return resolved != null && pages != null && pages.exists(resolved);
    }

    public static ResourceLocation resolve(String reference, MediaWikiTemplateContext context) {
        if (reference == null || reference.trim()
            .isEmpty()) {
            return null;
        }
        String target = reference.trim();
        ResourceLocation current = context.pageId();
        String namespace = current != null ? current.getResourceDomain() : context.sourcePack();
        if (target.startsWith("/") && current != null) {
            target = parentPath(current.getResourcePath()) + target.substring(1);
        }
        if (!target.endsWith(".md")) {
            target = target + ".md";
        }
        try {
            return new ResourceLocation(namespace, target);
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static String parentPath(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(0, slash + 1) : "";
    }

    @FunctionalInterface
    public interface PageCollectionLookup {

        boolean exists(ResourceLocation pageId);
    }
}
