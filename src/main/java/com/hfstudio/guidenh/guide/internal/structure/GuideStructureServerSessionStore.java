package com.hfstudio.guidenh.guide.internal.structure;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuideStructureServerSessionStore {

    private final GuideStructurePlacementService placementService;
    private final ConcurrentHashMap<UUID, GuideStructureMemoryStore> stores = new ConcurrentHashMap<>();
    private final GuideMemoryBudget sharedBudget = new GuideMemoryBudget(256L * 1024L * 1024L);

    public GuideStructureServerSessionStore(GuideStructurePlacementService placementService) {
        this.placementService = placementService;
    }

    public GuideStructureMemoryStore.Entry remember(UUID playerId, String label, String structureText)
        throws Exception {
        return getOrCreate(playerId).remember(label, structureText);
    }

    public List<GuideStructureData> snapshotData(UUID playerId) {
        return getOrCreate(playerId).snapshotData();
    }

    public int size(UUID playerId) {
        return getOrCreate(playerId).size();
    }

    public boolean isEmpty(UUID playerId) {
        return getOrCreate(playerId).isEmpty();
    }

    public void clear(UUID playerId) {
        GuideStructureMemoryStore store = stores.remove(playerId);
        if (store != null) store.clear();
    }

    public void reset(UUID playerId) {
        clear(playerId);
        stores.put(playerId, new GuideStructureMemoryStore(placementService, sharedBudget));
    }

    public void clear() {
        stores.values()
            .forEach(GuideStructureMemoryStore::clear);
        stores.clear();
    }

    private GuideStructureMemoryStore getOrCreate(UUID playerId) {
        return stores
            .computeIfAbsent(playerId, ignored -> new GuideStructureMemoryStore(placementService, sharedBudget));
    }
}
