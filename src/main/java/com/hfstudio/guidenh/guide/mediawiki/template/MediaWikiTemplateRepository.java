package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Every template currently available, rebuilt whenever the guide page set changes. Rebuild takes the
 * pages as an argument rather than reading a global so the expansion path stays testable without a
 * running client; language selection has already happened upstream, so the last definition stored for a
 * name is the one the active guide language resolved to.
 */
public class MediaWikiTemplateRepository {

    private static final Object LOCK = new Object();
    private static volatile Map<MediaWikiTemplateName, MediaWikiTemplateDefinition> templates = Map.of();
    private static volatile long revision;

    private MediaWikiTemplateRepository() {}

    public static void clear() {
        synchronized (LOCK) {
            templates = Map.of();
            revision++;
        }
    }

    public static void rebuild(Iterable<TemplatePageSource> pages) {
        Map<MediaWikiTemplateName, MediaWikiTemplateDefinition> rebuilt = new LinkedHashMap<>();
        for (TemplatePageSource page : pages) {
            if (page == null || page.pageId() == null) {
                continue;
            }
            String templateName = MediaWikiTemplatePageIds.templateNameOf(page.pageId());
            if (templateName == null) {
                continue;
            }
            MediaWikiTemplateName name = MediaWikiTemplateName.parse(templateName);
            rebuilt.put(name, new MediaWikiTemplateDefinition(name, page.pageId(), page.sourcePack(), page.language()));
        }
        synchronized (LOCK) {
            templates = Map.copyOf(rebuilt);
            revision++;
        }
    }

    public static @Nullable MediaWikiTemplateDefinition find(@Nullable MediaWikiTemplateName name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return templates.get(name);
    }

    public static @Nullable MediaWikiTemplateDefinition findByName(@Nullable String rawName) {
        return find(MediaWikiTemplateName.parse(rawName));
    }

    public static int size() {
        return templates.size();
    }

    public record TemplatePageSource(String sourcePack, String language, ResourceLocation pageId) {}
}
