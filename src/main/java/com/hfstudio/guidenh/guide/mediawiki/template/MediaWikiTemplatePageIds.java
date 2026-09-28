package com.hfstudio.guidenh.guide.mediawiki.template;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Owns the reserved {@code templates/} path prefix. Templates are ordinary guide pages under that
 * prefix so they inherit per-language folders, resource-pack priority, and live reload; reserving the
 * prefix is what keeps them from being authored as visible content.
 */
public class MediaWikiTemplatePageIds {

    public static final String TEMPLATE_FOLDER = "templates";
    private static final String PREFIX = TEMPLATE_FOLDER + "/";
    private static final String MARKDOWN_SUFFIX = ".md";

    private MediaWikiTemplatePageIds() {}

    public static boolean isTemplatePage(@Nullable ResourceLocation pageId) {
        return templateNameOf(pageId) != null;
    }

    public static @Nullable String templateNameOf(@Nullable ResourceLocation pageId) {
        if (pageId == null) {
            return null;
        }
        String path = pageId.getResourcePath();
        int marker = path.lastIndexOf(PREFIX);
        if (marker < 0) {
            return null;
        }
        int start = marker + PREFIX.length();
        if (start >= path.length() || !path.endsWith(MARKDOWN_SUFFIX)) {
            return null;
        }
        String name = path.substring(start, path.length() - MARKDOWN_SUFFIX.length());
        return name.isEmpty() ? null : name;
    }

    public static ResourceLocation pageIdFor(String namespace, String contentRootFolder, String language,
        String templateName) {
        String path = contentRootFolder + "/" + language + "/" + PREFIX + templateName + MARKDOWN_SUFFIX;
        return new ResourceLocation(namespace, path);
    }
}
