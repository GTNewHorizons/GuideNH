package com.hfstudio.guidenh.guide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.FrontmatterPageMeta;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuidePage {

    private final String sourcePack;
    private final ResourceLocation id;
    private final LytDocument document;
    private final List<LytGuidebookScene> scenes;
    private final Set<LytGuidebookScene> registeredScenes;
    private long registeredScenesContentRevision;
    @Nullable
    private final LytHeading titleHeading;
    @Nullable
    private final FrontmatterPageMeta pageMeta;

    public GuidePage(String sourcePack, ResourceLocation id, LytDocument document) {
        this(sourcePack, id, document, null, null);
    }

    public GuidePage(String sourcePack, ResourceLocation id, LytDocument document, @Nullable LytHeading titleHeading) {
        this(sourcePack, id, document, titleHeading, null);
    }

    public GuidePage(String sourcePack, ResourceLocation id, LytDocument document, @Nullable LytHeading titleHeading,
        @Nullable FrontmatterPageMeta pageMeta) {
        this.sourcePack = sourcePack;
        this.id = id;
        this.document = document;
        this.titleHeading = titleHeading;
        this.pageMeta = pageMeta;
        this.scenes = collectScenes(document);
        this.registeredScenes = Collections.newSetFromMap(new IdentityHashMap<LytGuidebookScene, Boolean>());
        this.registeredScenes.addAll(scenes);
        this.registeredScenesContentRevision = document.getContentRevision();
    }

    public String sourcePack() {
        return sourcePack;
    }

    public ResourceLocation id() {
        return id;
    }

    public LytDocument document() {
        return document;
    }

    public List<LytGuidebookScene> scenes() {
        return scenes;
    }

    public @Nullable LytHeading titleHeading() {
        return titleHeading;
    }

    @Nullable
    public FrontmatterPageMeta pageMeta() {
        return pageMeta;
    }

    public void prepareForDisplay() {
        document.setHoveredElement(null);
        for (var scene : scenes) {
            scene.resetInteractiveState();
        }
    }

    /**
     * Releases client-only preview worlds owned by this page while retaining the compiled
     * document tree. The page cache may evict or invalidate a page without discarding the
     * compiled nodes, so runtime scene resources need their own lifecycle.
     */
    public void releaseRuntimeScenes() {
        // MaterializeTask may append GameScene nodes after compilation. Discover those nodes before
        // releasing so asynchronously created preview worlds cannot outlive this page.
        registerMaterializedScenes(true);
        for (LytGuidebookScene scene : scenes) {
            if (scene != null) {
                GuidebookLevel level = scene.getLevel();
                if (level != null) {
                    level.releaseRuntimeWorld();
                }
            }
        }
    }

    /**
     * Adds scenes materialized into the document after the initial page compilation when its
     * content changed since the previous scan.
     *
     * @return the number of newly registered scenes
     */
    public int refreshMaterializedScenes() {
        return registerMaterializedScenes(false);
    }

    private int registerMaterializedScenes(boolean force) {
        if (document == null) {
            return 0;
        }
        long contentRevision = document.getContentRevision();
        if (!force && contentRevision == registeredScenesContentRevision) {
            return 0;
        }
        ArrayDeque<LytNode> pending = new ArrayDeque<>();
        pending.add(document);
        int added = 0;
        while (!pending.isEmpty()) {
            LytNode node = pending.removeLast();
            if (node instanceof LytGuidebookScene scene && registeredScenes.add(scene)) {
                scenes.add(scene);
                added++;
            }
            List<? extends LytNode> children = node.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                pending.addLast(children.get(i));
            }
        }
        registeredScenesContentRevision = contentRevision;
        return added;
    }

    private static List<LytGuidebookScene> collectScenes(LytDocument document) {
        ArrayList<LytGuidebookScene> scenes = new ArrayList<>();
        ArrayDeque<LytNode> pending = new ArrayDeque<>();
        pending.add(document);

        while (!pending.isEmpty()) {
            var node = pending.removeLast();
            if (node instanceof LytGuidebookScene scene) {
                scenes.add(scene);
            }

            var children = node.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                pending.addLast(children.get(i));
            }
        }

        return scenes;
    }
}
