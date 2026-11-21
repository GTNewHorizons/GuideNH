package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

public class GuideSiteTemplateRegistry {

    private int counter = 1;
    private final List<String> rendered = new ArrayList<>();
    private final Map<String, String> idsByHtml = new LinkedHashMap<>();
    private final Map<String, String> idsBySemanticKey = new LinkedHashMap<>();
    private final Map<String, String> localRenderedBySemanticKey = new LinkedHashMap<>();
    private final Map<String, String> renderedBySemanticKey;
    private final Map<String, String> itemHrefs = new LinkedHashMap<>();
    private final Function<ItemStack, String> itemHrefResolver;
    private final Function<String, String> fluidHrefResolver;
    private final Map<String, String> fluidHrefs = new LinkedHashMap<>();
    private final Function<String, List<Map<String, String>>> fluidTargetResolver;
    private final Map<String, List<Map<String, String>>> fluidTargets = new LinkedHashMap<>();

    public GuideSiteTemplateRegistry() {
        this(new LinkedHashMap<>());
    }

    public GuideSiteTemplateRegistry(Map<String, String> renderedBySemanticKey) {
        this(renderedBySemanticKey, stack -> "");
    }

    public GuideSiteTemplateRegistry(Map<String, String> renderedBySemanticKey,
        Function<ItemStack, String> itemHrefResolver) {
        this(renderedBySemanticKey, itemHrefResolver, fluid -> "");
    }

    public GuideSiteTemplateRegistry(Map<String, String> renderedBySemanticKey,
        Function<ItemStack, String> itemHrefResolver, Function<String, String> fluidHrefResolver) {
        this(renderedBySemanticKey, itemHrefResolver, fluidHrefResolver, fluid -> List.of());
    }

    public GuideSiteTemplateRegistry(Map<String, String> renderedBySemanticKey,
        Function<ItemStack, String> itemHrefResolver, Function<String, String> fluidHrefResolver,
        Function<String, List<Map<String, String>>> fluidTargetResolver) {
        this.renderedBySemanticKey = renderedBySemanticKey != null ? renderedBySemanticKey : new LinkedHashMap<>();
        this.itemHrefResolver = itemHrefResolver;
        this.fluidHrefResolver = fluidHrefResolver;
        this.fluidTargetResolver = fluidTargetResolver;
    }

    public List<Map<String, String>> resolveFluidTargets(String id) {
        if (id == null || fluidTargetResolver == null) return List.of();
        return fluidTargets.computeIfAbsent(id, fluid -> {
            var targets = fluidTargetResolver.apply(fluid);
            return targets == null ? List.of()
                : targets.stream()
                    .map(Map::copyOf)
                    .toList();
        });
    }

    public String resolveFluidHref(String id) {
        if (id == null || fluidHrefResolver == null) return "";
        return fluidHrefs.computeIfAbsent(id, fluid -> {
            String href = fluidHrefResolver.apply(fluid);
            return href == null ? "" : href;
        });
    }

    public String resolveItemHref(ItemStack stack) {
        String key = GuideSiteItemSupport.tooltipCacheKey(stack);
        if (key == null || itemHrefResolver == null) {
            return "";
        }
        return itemHrefs.computeIfAbsent(key, ignored -> {
            String href = itemHrefResolver.apply(stack);
            return href != null ? href : "";
        });
    }

    public String getOrComputeRendered(String semanticKey, Supplier<String> renderer) {
        if (semanticKey == null || semanticKey.isEmpty() || renderer == null) {
            return renderer != null ? renderer.get() : "";
        }
        String existing = renderedBySemanticKey.get(semanticKey);
        if (existing != null) {
            return existing;
        }
        String html = renderer.get();
        renderedBySemanticKey.put(semanticKey, html != null ? html : "");
        return html != null ? html : "";
    }

    /**
     * Caches fragments containing template references only within this page.
     */
    public String getOrComputeLocalRendered(String semanticKey, Supplier<String> renderer) {
        String existing = localRenderedBySemanticKey.get(semanticKey);
        if (existing != null) {
            return existing;
        }
        String html = renderer.get();
        String renderedHtml = html != null ? html : "";
        localRenderedBySemanticKey.put(semanticKey, renderedHtml);
        return renderedHtml;
    }

    public String getOrCreate(String semanticKey, Supplier<String> renderer) {
        if (semanticKey != null && !semanticKey.isEmpty()) {
            String existing = idsBySemanticKey.get(semanticKey);
            if (existing != null) {
                return existing;
            }
        }
        String html = renderer.get();
        String id = html != null && !html.isBlank() ? create(html) : "";
        if (semanticKey != null && !semanticKey.isEmpty()) {
            idsBySemanticKey.put(semanticKey, id);
        }
        return id;
    }

    public String create(String html) {
        String existing = idsByHtml.get(html);
        if (existing != null) {
            return existing;
        }

        String id = "tmpl-" + counter++;
        idsByHtml.put(html, id);
        rendered.add("<template id=\"" + id + "\">" + html + "</template>");
        return id;
    }

    public List<String> renderAll() {
        return rendered;
    }
}
