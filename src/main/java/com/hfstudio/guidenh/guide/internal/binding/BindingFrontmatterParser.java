package com.hfstudio.guidenh.guide.internal.binding;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

public class BindingFrontmatterParser {

    protected BindingFrontmatterParser() {}

    public static List<Pair<BindingKey, PageAnchor>> read(ParsedGuidePage page) {
        Map<String, Object> properties = page.getFrontmatter()
            .additionalProperties();
        var result = new LinkedHashSet<Pair<BindingKey, PageAnchor>>();
        appendAlias(page, result, properties.get("research_id"), "thaumcraft", "research_id", false);
        appendAlias(page, result, properties.get("research_ids"), "thaumcraft", "research_ids", true);
        Object raw = properties.get("bindings");
        if (raw != null && !(raw instanceof List<?>)) warn(page, "bindings", raw);
        if (raw instanceof List<?>entries) {
            for (Object entry : entries) {
                if (!(entry instanceof Map<?, ?>map) || !(map.get("type") instanceof String type)
                    || !(map.get("id") instanceof String id)
                    || map.keySet()
                        .stream()
                        .anyMatch(
                            key -> !List.of("type", "id", "anchor")
                                .contains(key))) {
                    warn(page, "bindings", entry);
                    continue;
                }
                Object anchor = map.get("anchor");
                if (anchor != null && (!(anchor instanceof String text) || text.trim()
                    .isEmpty())) {
                    warn(page, "bindings.anchor", anchor);
                    continue;
                }
                append(page, result, type, id, anchor instanceof String text ? text.trim() : null, "bindings");
            }
        }
        return List.copyOf(result);
    }

    private static void appendAlias(ParsedGuidePage page, LinkedHashSet<Pair<BindingKey, PageAnchor>> result,
        Object value, String type, String property, boolean list) {
        if (value == null) return;
        if (list && value instanceof List<?>values) {
            for (Object entry : values) {
                if (entry instanceof String id) append(page, result, type, id, null, property);
                else warn(page, property, entry);
            }
        } else if (!list && value instanceof String id) append(page, result, type, id, null, property);
        else warn(page, property, value);
    }

    private static void append(ParsedGuidePage page, LinkedHashSet<Pair<BindingKey, PageAnchor>> result, String type,
        String id, String anchor, String property) {
        try {
            result.add(Pair.of(BindingKey.parse(type, id), new PageAnchor(page.getId(), anchor)));
        } catch (IllegalArgumentException | NullPointerException exception) {
            warn(page, property, id);
        }
    }

    private static void warn(ParsedGuidePage page, String property, Object value) {
        GuideDebugLog.warn("[GuideNH] Page {} contains an invalid {} binding: {}", page.getId(), property, value);
    }
}
