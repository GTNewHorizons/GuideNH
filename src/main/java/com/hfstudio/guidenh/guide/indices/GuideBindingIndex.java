package com.hfstudio.guidenh.guide.indices;

import java.util.LinkedHashSet;
import java.util.List;

import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.binding.BindingFrontmatterParser;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

public class GuideBindingIndex extends MultiValuedIndex<BindingKey, PageAnchor> {

    private long revision;
    private List<BindingKey> keys = List.of();

    public GuideBindingIndex() {
        super("Binding Index", BindingFrontmatterParser::read, (writer, key) -> {
            writer.beginObject();
            writer.name("type")
                .value(key.type());
            writer.name("id")
                .value(key.id());
            writer.endObject();
        }, (writer, anchor) -> writer.value(anchor.toString()));
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        super.rebuild(pages);
        updateKeys();
    }

    @Override
    public void update(List<ParsedGuidePage> pages, List<GuidePageChange> changes) {
        super.update(pages, changes);
        updateKeys();
    }

    public long getRevision() {
        return revision;
    }

    @Override
    public List<BindingKey> keys() {
        return keys;
    }

    public List<PageAnchor> findAll(BindingKey key) {
        return key == null ? List.of() : List.copyOf(new LinkedHashSet<>(get(key)));
    }

    private void updateKeys() {
        keys = super.keys();
        revision++;
    }
}
