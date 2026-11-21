package com.hfstudio.guidenh.integration.betterquesting;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.GuideBindingIndex;
import com.hfstudio.guidenh.guide.indices.UniqueIndex;
import com.hfstudio.guidenh.guide.internal.binding.BindingFrontmatterParser;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

/**
 * Retains the UUID lookup contract while sharing the complete multi-page binding index.
 */
public class QuestIndex extends UniqueIndex<UUID, PageAnchor> {

    private final GuideBindingIndex bindings;
    private final boolean ownsIndex;

    public QuestIndex() {
        this(new GuideBindingIndex(), true);
    }

    public QuestIndex(GuideBindingIndex bindings) {
        this(bindings, false);
    }

    private QuestIndex(GuideBindingIndex bindings, boolean ownsIndex) {
        super(
            "Quest Index",
            QuestIndex::getQuestAnchors,
            (writer, value) -> writer.value(value.toString()),
            (writer, value) -> writer.value(value.toString()));
        this.bindings = bindings;
        this.ownsIndex = ownsIndex;
    }

    @Override
    @Nullable
    public PageAnchor get(UUID id) {
        var pages = findAllByUuid(id);
        return pages.isEmpty() ? null : pages.getFirst();
    }

    @Nullable
    public PageAnchor findByUuid(@Nullable UUID id) {
        return id == null ? null : get(id);
    }

    @Nullable
    public PageAnchor findByUuidString(@Nullable String id) {
        return findByUuid(QuestIdParser.parse(id));
    }

    public List<PageAnchor> findAllByUuid(@Nullable UUID id) {
        return id == null ? List.of() : bindings.findAll(new BindingKey("betterquesting", id.toString()));
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        if (ownsIndex) bindings.rebuild(pages);
    }

    @Override
    public void update(List<ParsedGuidePage> pages, List<GuidePageChange> changes) {
        if (ownsIndex) bindings.update(pages, changes);
    }

    @Override
    public void export(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (var key : bindings.keys()) {
            if (!"betterquesting".equals(key.type())) continue;
            var pages = bindings.findAll(key);
            if (pages.isEmpty()) continue;
            writer.value(key.id());
            writer.value(
                pages.getFirst()
                    .toString());
        }
        writer.endArray();
    }

    public static List<Pair<UUID, PageAnchor>> getQuestAnchors(ParsedGuidePage page) {
        return BindingFrontmatterParser.read(page)
            .stream()
            .filter(
                entry -> "betterquesting".equals(
                    entry.getKey()
                        .type()))
            .map(
                entry -> Pair.of(
                    UUID.fromString(
                        entry.getKey()
                            .id()),
                    entry.getValue()))
            .toList();
    }
}
