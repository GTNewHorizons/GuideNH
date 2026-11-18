package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class FluidIndex extends MultiValuedIndex<String, PageAnchor> {

    private long revision;

    public FluidIndex() {
        super(
            "Fluid Index",
            FluidIndex::getFluidAnchors,
            JsonWriter::value,
            (writer, value) -> writer.value(value.toString()));
    }

    public long getRevision() {
        return revision;
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        super.rebuild(pages);
        revision++;
    }

    @Override
    public void update(List<ParsedGuidePage> allPages, List<GuidePageChange> changes) {
        super.update(allPages, changes);
        revision++;
    }

    public static List<Pair<String, PageAnchor>> getFluidAnchors(ParsedGuidePage page) {
        var properties = page.getFrontmatter()
            .additionalProperties();
        var bindings = new LinkedHashSet<String>();
        Object single = properties.get("fluid_id");
        if (single != null) addBinding(page, single, bindings);
        Object multiple = properties.get("fluid_ids");
        if (multiple instanceof List<?>list) {
            for (Object entry : list) addBinding(page, entry, bindings);
        } else if (multiple != null) {
            GuideDebugLog.warn("[GuideNH] Page {} contains malformed fluid_ids frontmatter", page.getId());
        }
        var entries = new ArrayList<Pair<String, PageAnchor>>(bindings.size());
        for (String binding : bindings) {
            int separator = binding.indexOf('#');
            String id = separator < 0 ? binding
                : binding.substring(0, separator)
                    .trim();
            String anchor = separator < 0 ? null
                : binding.substring(separator + 1)
                    .trim();
            entries.add(Pair.of(id, new PageAnchor(page.getId(), anchor == null || anchor.isEmpty() ? null : anchor)));
        }
        return entries;
    }

    private static void addBinding(ParsedGuidePage page, Object value, LinkedHashSet<String> bindings) {
        if (value instanceof String text && !text.trim()
            .isEmpty()
            && !text.trim()
                .startsWith("#")) {
            int separator = text.indexOf('#');
            String id = (separator < 0 ? text : text.substring(0, separator)).trim();
            String anchor = separator < 0 ? ""
                : text.substring(separator + 1)
                    .trim();
            bindings.add(anchor.isEmpty() ? id : id + "#" + anchor);
        } else {
            GuideDebugLog.warn("[GuideNH] Page {} contains an invalid fluid binding: {}", page.getId(), value);
        }
    }
}
