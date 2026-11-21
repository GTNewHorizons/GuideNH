package com.hfstudio.guidenh.guide.scene.level;

import java.util.Collection;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

public interface GuidebookPreviewWorld {

    /**
     * Monotonic structure mutation revision, or zero when the world does not expose one.
     */
    default long getPreviewStructureRevision() {
        return 0L;
    }

    /**
     * Releases client renderer/player back-references before the owning level is discarded.
     */
    void closePreviewWorld();

    void syncLoadedTileEntities(Collection<TileEntity> tileEntities);

    void syncLoadedEntities(Collection<Entity> entities);

    void updateEntitiesForPreview();
}
