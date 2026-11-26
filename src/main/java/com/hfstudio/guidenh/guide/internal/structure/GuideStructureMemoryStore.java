package com.hfstudio.guidenh.guide.internal.structure;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class GuideStructureMemoryStore {

    private final GuideStructurePlacementService placementService;
    private final List<Entry> entries = new ArrayList<>();
    private final GuideMemoryBudget sharedBudget;
    private long totalBytes;

    public static final int MAX_ENTRIES = 64;
    public static final long MAX_BYTES = 64L * 1024L * 1024L;

    public GuideStructureMemoryStore(GuideStructurePlacementService placementService) {
        this(placementService, new GuideMemoryBudget(MAX_BYTES));
    }

    public GuideStructureMemoryStore(GuideStructurePlacementService placementService, GuideMemoryBudget sharedBudget) {
        this.placementService = placementService;
        this.sharedBudget = sharedBudget;
    }

    public synchronized Entry remember(String label, String structureText) throws Exception {
        if (entries.size() >= MAX_ENTRIES || totalBytes >= MAX_BYTES) {
            throw new IllegalStateException("Structure memory exceeds its retention budget");
        }
        GuideStructureData data = placementService.parse(structureText);
        Entry entry = new Entry(this, label, structureText, data);
        add(entry);
        return entry;
    }

    public synchronized Entry remember(String label, GuideStructureData data) {
        Entry entry = new Entry(this, label, null, data);
        add(entry);
        return entry;
    }

    public synchronized List<Entry> snapshotEntries() {
        return new ArrayList<>(entries);
    }

    public synchronized List<GuideStructureData> snapshotData() {
        List<GuideStructureData> snapshot = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            snapshot.add(entry.getData());
        }
        return snapshot;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    public synchronized void clear() {
        entries.clear();
        sharedBudget.release(totalBytes);
        totalBytes = 0L;
    }

    private void add(Entry entry) {
        long bytes = GuideNbtMemoryUsage.estimate(entry.data.getRoot())
            + (entry.structureText == null ? 0L : (long) entry.structureText.length() * 2L);
        if (entries.size() >= MAX_ENTRIES || bytes > MAX_BYTES - totalBytes || !sharedBudget.reserve(bytes)) {
            throw new IllegalStateException("Structure memory exceeds its retention budget");
        }
        entries.add(entry);
        totalBytes += bytes;
    }

    private synchronized String resolveText(Entry entry) {
        if (entry.structureText != null) return entry.structureText;
        String text = GuideTextNbtCodec.writeStructureSnbt(entry.data.getRoot());
        long bytes = (long) text.length() * 2L;
        if (entries.contains(entry) && bytes <= MAX_BYTES - totalBytes && sharedBudget.reserve(bytes)) {
            entry.structureText = text;
            totalBytes += bytes;
        }
        return text;
    }

    public static class Entry {

        private final GuideStructureMemoryStore owner;

        @Getter
        private final String label;
        @Getter
        private final GuideStructureData data;
        @Nullable
        private volatile String structureText;

        private Entry(GuideStructureMemoryStore owner, String label, @Nullable String structureText,
            GuideStructureData data) {
            this.owner = owner;
            this.label = label;
            this.structureText = structureText;
            this.data = data;
        }

        public String getStructureText() {
            return structureText != null ? structureText : owner.resolveText(this);
        }

    }
}
