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
    private static volatile List<MediaWikiTemplateDefinition> sortedTemplates = List.of();

    private MediaWikiTemplateRepository() {}

    public static void clear() {
        synchronized (LOCK) {
            templates = Map.of();
            sortedTemplates = List.of();
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
                    page.guideId(),
                    page.sourcePack(),
                    page.language(),
                    page.parameters()));
        }
        synchronized (LOCK) {
            templates = Map.copyOf(rebuilt);
            sortedTemplates = rebuilt.values()
                .stream()
                .sorted(
                    Comparator.comparing(
                        definition -> definition.name()
                            .value(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
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

    public static List<MediaWikiTemplateDefinition> all() {
        return sortedTemplates;
    }

    public record TemplatePageSource(@Nullable ResourceLocation guideId, String sourcePack, String language,
        ResourceLocation pageId, MediaWikiTemplateParameters parameters) {

        public TemplatePageSource(String sourcePack, String language, ResourceLocation pageId,
            MediaWikiTemplateParameters parameters) {
            this(null, sourcePack, language, pageId, parameters);
        }

        public TemplatePageSource(String sourcePack, String language, ResourceLocation pageId) {
            this(null, sourcePack, language, pageId, MediaWikiTemplateParameters.EMPTY);
        }
    }
}
