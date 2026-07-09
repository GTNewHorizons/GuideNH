package com.hfstudio.guidenh.guide.internal.structure;

import lombok.Getter;
import lombok.Setter;

public class GuideNhStructureRuntime {

    public static final GuideStructurePlacementService PLACEMENT_SERVICE = new GuideStructurePlacementService();
    public static final GuideStructureMemoryStore CLIENT_MEMORY_STORE = new GuideStructureMemoryStore(
        PLACEMENT_SERVICE);
    public static final GuideStructureServerSessionStore SERVER_SESSION_STORE = new GuideStructureServerSessionStore(
        PLACEMENT_SERVICE);

    @Getter
    @Setter
    public static volatile boolean serverStructureCommandsAvailable = false;
    @Getter
    @Setter
    public static volatile boolean clientStructureSyncNeeded = false;

    private GuideNhStructureRuntime() {}

    public static GuideStructurePlacementService getPlacementService() {
        return PLACEMENT_SERVICE;
    }

    public static GuideStructureMemoryStore getClientMemoryStore() {
        return CLIENT_MEMORY_STORE;
    }

    public static GuideStructureServerSessionStore getServerSessionStore() {
        return SERVER_SESSION_STORE;
    }

}
