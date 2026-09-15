package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class MediaWikiTemplateDependencyGraph {

    private static final Object LOCK = new Object();

    private static Map<ResourceLocation, Set<MediaWikiTemplateName>> byPage = Map.of();
    private static Map<MediaWikiTemplateName, Set<ResourceLocation>> byTemplate = Map.of();

    private MediaWikiTemplateDependencyGraph() {}

    public static void clear() {
        synchronized (LOCK) {
            byPage = Map.of();
            byTemplate = Map.of();
        }
    }

    public static void recordPage(ResourceLocation pageId, Set<MediaWikiTemplateName> used) {
        if (pageId == null) {
            return;
        }
        synchronized (LOCK) {
            Map<ResourceLocation, Set<MediaWikiTemplateName>> pages = new HashMap<>(byPage);
            Map<MediaWikiTemplateName, Set<ResourceLocation>> templates = new HashMap<>(byTemplate);

            Set<MediaWikiTemplateName> previous = pages.remove(pageId);
            if (previous != null) {
                for (MediaWikiTemplateName name : previous) {
                    Set<ResourceLocation> users = templates.get(name);
                    if (users != null) {
                        Set<ResourceLocation> remaining = new HashSet<>(users);
                        remaining.remove(pageId);
                        if (remaining.isEmpty()) {
                            templates.remove(name);
                        } else {
                            templates.put(name, remaining);
                        }
                    }
                }
            }

            if (used != null && !used.isEmpty()) {
                Set<MediaWikiTemplateName> recorded = new LinkedHashSet<>(used);
                pages.put(pageId, recorded);
                for (MediaWikiTemplateName name : recorded) {
                    link(templates, name, pageId);
                }
            }
            byPage = Map.copyOf(pages);
            byTemplate = copyOfSets(templates);
        }
    }

    public static void addEdge(ResourceLocation pageId, MediaWikiTemplateName used) {
        if (pageId == null || used == null || used.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            Set<MediaWikiTemplateName> existing = byPage.get(pageId);
            if (existing != null && existing.contains(used)) {
                return;
            }
            Map<ResourceLocation, Set<MediaWikiTemplateName>> pages = new HashMap<>(byPage);
            Map<MediaWikiTemplateName, Set<ResourceLocation>> templates = new HashMap<>(byTemplate);
            pages.computeIfAbsent(pageId, key -> new LinkedHashSet<>())
                .add(used);
            link(templates, used, pageId);
            byPage = Map.copyOf(pages);
            byTemplate = copyOfSets(templates);
        }
    }

    private static void link(Map<MediaWikiTemplateName, Set<ResourceLocation>> templates, MediaWikiTemplateName name,
        ResourceLocation pageId) {
        templates.computeIfAbsent(name, key -> new LinkedHashSet<>())
            .add(pageId);
    }

    public static Set<ResourceLocation> dependentsOf(@Nullable MediaWikiTemplateName name) {
        if (name == null || name.isEmpty()) {
            return Set.of();
        }
        synchronized (LOCK) {
            Set<ResourceLocation> direct = byTemplate.get(name);
            if (direct == null || direct.isEmpty()) {
                return Set.of();
            }
            Set<ResourceLocation> collected = new LinkedHashSet<>();
            for (ResourceLocation page : direct) {
                collectTransitively(page, collected);
            }
            return collected;
        }
    }

    private static void collectTransitively(ResourceLocation page, Set<ResourceLocation> collected) {
        if (!collected.add(page)) {
            return;
        }
        String templateName = MediaWikiTemplatePageIds.templateNameOf(page);
        if (templateName == null) {
            return;
        }
        Set<ResourceLocation> users = byTemplate.get(MediaWikiTemplateName.parse(templateName));
        if (users == null) {
            return;
        }
        for (ResourceLocation user : users) {
            collectTransitively(user, collected);
        }
    }

    public static Set<MediaWikiTemplateName> usedBy(@Nullable ResourceLocation pageId) {
        if (pageId == null) {
            return Set.of();
        }
        Set<MediaWikiTemplateName> used = byPage.get(pageId);
        return used == null ? Set.of() : Set.copyOf(used);
    }

    public static int edgeCount() {
        int total = 0;
        for (Set<ResourceLocation> users : byTemplate.values()) {
            total += users.size();
        }
        return total;
    }

    public static int pageCount() {
        return byPage.size();
    }

    private static Map<MediaWikiTemplateName, Set<ResourceLocation>> copyOfSets(
        Map<MediaWikiTemplateName, Set<ResourceLocation>> source) {
        Map<MediaWikiTemplateName, Set<ResourceLocation>> copy = new LinkedHashMap<>();
        for (Map.Entry<MediaWikiTemplateName, Set<ResourceLocation>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}
