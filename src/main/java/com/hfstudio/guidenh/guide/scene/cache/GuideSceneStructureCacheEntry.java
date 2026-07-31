package com.hfstudio.guidenh.guide.scene.cache;

import java.io.Serial;
import java.io.Serializable;

import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuideSceneStructureCacheEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final GuideSceneStructureSnapshot levelSnapshot;

    public GuideSceneStructureCacheEntry(GuideSceneStructureSnapshot levelSnapshot) {
        this.levelSnapshot = levelSnapshot;
    }

    public static GuideSceneStructureCacheEntry capture(LytGuidebookScene scene) {
        GuidebookLevel level = scene.getLevel();
        return new GuideSceneStructureCacheEntry(GuideSceneStructureSnapshot.capture(level));
    }

    public GuidebookLevel restoreLevel() {
        return levelSnapshot.restoreLevel();
    }

    public void restoreInto(LytGuidebookScene scene) {
        scene.setLevel(restoreLevel());
        scene.setStructureLibSceneMetadata(null);
    }
}
