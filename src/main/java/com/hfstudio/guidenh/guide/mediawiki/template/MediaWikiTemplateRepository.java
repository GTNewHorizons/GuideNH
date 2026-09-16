package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
            rebuilt.put(
                name,
                new MediaWikiTemplateDefinition(
                    name,
                    page.pageId(),
                    page.sourcePack(),
                    page.language(),
                    page.parameters()));
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

    /** Every template, ordered by name, for lists that show what the guide defines. */
    public static List<MediaWikiTemplateDefinition> all() {
        return templates.values()
            .stream()
            .sorted(
                Comparator.comparing(
                    definition -> definition.name()
                        .value(),
                    String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public record TemplatePageSource(String sourcePack, String language, ResourceLocation pageId,
        MediaWikiTemplateParameters parameters) {

        public TemplatePageSource(String sourcePack, String language, ResourceLocation pageId) {
            this(sourcePack, language, pageId, MediaWikiTemplateParameters.EMPTY);
        }
    }
}
