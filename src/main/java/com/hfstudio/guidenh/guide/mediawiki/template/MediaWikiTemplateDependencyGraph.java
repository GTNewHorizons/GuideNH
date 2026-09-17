package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class MediaWikiTemplateDependencyGraph {

    private static final Object LOCK = new Object();

    private static final Map<ResourceLocation, Set<MediaWikiTemplateName>> byPage = new HashMap<>();
    private static final Map<MediaWikiTemplateName, Set<ResourceLocation>> byTemplate = new HashMap<>();

    private MediaWikiTemplateDependencyGraph() {}

    public static void clear() {
        synchronized (LOCK) {
            byPage.clear();
            byTemplate.clear();
        }
    }

    public static void recordPage(ResourceLocation pageId, Set<MediaWikiTemplateName> used) {
        if (pageId == null) {
            return;
        }
        synchronized (LOCK) {
            Set<MediaWikiTemplateName> previous = byPage.remove(pageId);
            if (previous != null) {
                for (MediaWikiTemplateName name : previous) {
                    Set<ResourceLocation> users = byTemplate.get(name);
                    if (users != null) {
                        users.remove(pageId);
                        if (users.isEmpty()) {
                            byTemplate.remove(name);
                        }
                    }
                }
            }

            if (used != null && !used.isEmpty()) {
                Set<MediaWikiTemplateName> recorded = new LinkedHashSet<>(used);
                byPage.put(pageId, recorded);
                for (MediaWikiTemplateName name : recorded) {
                    link(name, pageId);
                }
            }
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
            byPage.computeIfAbsent(pageId, key -> new LinkedHashSet<>())
                .add(used);
            link(used, pageId);
        }
    }

    private static void link(MediaWikiTemplateName name, ResourceLocation pageId) {
        byTemplate.computeIfAbsent(name, key -> new LinkedHashSet<>())
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
        synchronized (LOCK) {
            Set<MediaWikiTemplateName> used = byPage.get(pageId);
            return used == null ? Set.of() : Set.copyOf(used);
        }
    }

    public static int edgeCount() {
        synchronized (LOCK) {
            int total = 0;
            for (Set<ResourceLocation> users : byTemplate.values()) {
                total += users.size();
            }
            return total;
        }
    }

    public static int pageCount() {
        synchronized (LOCK) {
            return byPage.size();
        }
    }
}
