package com.hfstudio.guidenh.guide.internal.editor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExportMode;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExporter;
import com.hfstudio.guidenh.guide.internal.item.RegionWandSelection;

import lombok.Getter;

public class SceneEditorOpenService {

    private final SceneEditorStructureCache structureCache;

    public SceneEditorOpenService() {
        this(SceneEditorStructureCache.createDefault());
    }

    public SceneEditorOpenService(SceneEditorStructureCache structureCache) {
        this.structureCache = structureCache;
    }

    public OpenResult createInitialSession(@Nullable EntityPlayer player) {
        if (player == null) {
            return new OpenResult(SceneEditorSession.createBlank(), false, null);
        }

        if (!RegionWandSelection.hasCompleteSelection()) {
            return createInitialSession(false, null);
        }

        RegionWandExportMode mode = RegionWandExporter.getExportMode();
        // blocks/blocks_e modes generate <GameScene><Block> MDX, not SNBT ImportStructure.
        // Open blank so the editor doesn't pre-fill with the wrong format.
        if (mode == RegionWandExportMode.BLOCKS || mode == RegionWandExportMode.BLOCKS_ENTITIES) {
            return new OpenResult(SceneEditorSession.createBlank(), false, null);
        }

        boolean includeEntities = mode.includeEntities();
        String structureSnbt = RegionWandExporter.exportSelectionAsStructureSnbt(player.worldObj, includeEntities);
        return createInitialSession(true, structureSnbt);
    }

    @Nullable
    public ServerSelectionRequest createServerSelectionRequest(@Nullable EntityPlayer player) {
        if (player == null || !RegionWandSelection.hasCompleteSelection()) {
            return null;
        }
        RegionWandExportMode mode = RegionWandExporter.getExportMode();
        if (mode == RegionWandExportMode.BLOCKS || mode == RegionWandExportMode.BLOCKS_ENTITIES) {
            return null;
        }
        RegionWandSelection.Bounds bounds = RegionWandSelection.getBounds();
        if (bounds == null) {
            return null;
        }
        return new ServerSelectionRequest(
            bounds.minX(),
            bounds.minY(),
            bounds.minZ(),
            bounds.sizeX(),
            bounds.sizeY(),
            bounds.sizeZ(),
            mode.includeEntities());
    }

    OpenResult createInitialSession(@Nullable ItemStack held, @Nullable String structureSnbt) {
        boolean canImportSelection = RegionWandSelection.isBound(held) && RegionWandSelection.hasCompleteSelection();
        return createInitialSession(canImportSelection, structureSnbt);
    }

    OpenResult createInitialSession(boolean canImportSelection, @Nullable String structureSnbt) {
        if (!canImportSelection || structureSnbt == null || structureSnbt.isEmpty()) {
            return new OpenResult(SceneEditorSession.createBlank(), true, GuidebookText.SceneEditorImportUnavailable);
        }

        SceneEditorSession session = SceneEditorSession.createImported(structureCache.createStructureSource());
        session.setImportedStructureSnbt(structureSnbt);
        applyImportedStructureDefaults(session, structureSnbt);
        return new OpenResult(session, false, GuidebookText.SceneEditorImportedSession);
    }

    void applyImportedStructureDefaults(SceneEditorSession session, String structureSnbt) {
        float[] structureCenter = extractStructureCenter(structureSnbt);
        if (structureCenter == null) {
            return;
        }
        session.getSceneModel()
            .setCenterX(structureCenter[0]);
        session.getSceneModel()
            .setCenterY(structureCenter[1]);
        session.getSceneModel()
            .setCenterZ(structureCenter[2]);
    }

    private float @Nullable [] extractStructureCenter(String structureSnbt) {
        try {
            NBTBase parsed = JsonToNBT.func_150315_a(structureSnbt);
            if (!(parsed instanceof NBTTagCompound root)) {
                return null;
            }
            int[] size = root.getIntArray("size");
            if (size.length < 3) {
                return null;
            }
            return new float[] { size[0] * 0.5f, size[1] * 0.5f, size[2] * 0.5f };
        } catch (Exception ignored) {
            return null;
        }
    }

    public static class OpenResult {

        @Getter
        private final SceneEditorSession session;
        @Getter
        private final boolean importUnavailable;
        @Nullable
        private final GuidebookText openFeedbackMessage;

        private OpenResult(SceneEditorSession session, boolean importUnavailable,
            @Nullable GuidebookText openFeedbackMessage) {
            this.session = session;
            this.importUnavailable = importUnavailable;
            this.openFeedbackMessage = openFeedbackMessage;
        }

        @Nullable
        public GuidebookText getOpenFeedbackMessage() {
            return openFeedbackMessage;
        }
    }

    @Getter
    public static class ServerSelectionRequest {

        private final int x;
        private final int y;
        private final int z;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final boolean includeEntities;

        private ServerSelectionRequest(int x, int y, int z, int sizeX, int sizeY, int sizeZ, boolean includeEntities) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.includeEntities = includeEntities;
        }

    }
}
