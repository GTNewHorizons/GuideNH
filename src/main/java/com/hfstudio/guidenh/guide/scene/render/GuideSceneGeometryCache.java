package com.hfstudio.guidenh.guide.scene.render;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.init.Blocks;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneTessellatorCapture;

public class GuideSceneGeometryCache {

    private static final int MIN_BLOCK_COUNT = 128;
    // Limit retained driver geometry; larger scenes still render their complete contents dynamically.
    private static final int MAX_BLOCK_COUNT = 1_000_000;
    private static final Map<Block, Class<? extends Block>> STATIC_BLOCKS = Map.of(
        Blocks.stone,
        BlockStone.class,
        Blocks.stained_glass,
        BlockStainedGlass.class,
        Blocks.ice,
        BlockIce.class,
        Blocks.water,
        BlockStaticLiquid.class,
        Blocks.stone_slab,
        BlockStoneSlab.class,
        Blocks.oak_stairs,
        BlockStairs.class,
        Blocks.iron_bars,
        BlockPane.class,
        Blocks.fence,
        BlockFence.class,
        Blocks.leaves,
        BlockOldLeaf.class);

    private WeakReference<GuidebookLevel> owner = new WeakReference<>(null);
    private final PassGeometry[] passes = { new PassGeometry(), new PassGeometry() };
    private long revision;
    private boolean eligible;
    private boolean fancyGraphics;
    private boolean anaglyph;

    public boolean render(GuidebookLevel level, List<int[]> blocks, int pass, boolean renderAllFaces,
        @Nullable List<int[]> translucentBlocks, BooleanSupplier drawGeometry) {
        if (pass < 0 || pass >= passes.length || GuideSiteSceneTessellatorCapture.getActive() != null) {
            return false;
        }
        var settings = Minecraft.getMinecraft().gameSettings;
        long currentRevision = level.getSpatialRevision();
        if (owner.get() != level || revision != currentRevision
            || fancyGraphics != settings.fancyGraphics
            || anaglyph != settings.anaglyph) {
            clear();
            owner = new WeakReference<>(level);
            revision = currentRevision;
            fancyGraphics = settings.fancyGraphics;
            anaglyph = settings.anaglyph;
            eligible = isEligible(level);
        }
        if (!eligible) {
            return false;
        }
        PassGeometry geometry = passes[pass];
        // Admit translucent geometry only after its exact order repeats. Rotation must not recompile every frame.
        if (pass == 1 && !geometry.matchesCoordinates(blocks, renderAllFaces)) {
            geometry.deleteDisplayList();
            geometry.capture(blocks, renderAllFaces, null);
            return false;
        }
        if (!geometry.matches(blocks, renderAllFaces)) {
            geometry.clear();
            int displayList = GLAllocation.generateDisplayLists(1);
            if (displayList == 0) {
                eligible = false;
                return false;
            }
            geometry.displayList = displayList;
            boolean compiled = false;
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            try {
                // A failed block renderer must never leave an incomplete display list reusable.
                compiled = drawGeometry.getAsBoolean() && level.getSpatialRevision() == revision;
            } finally {
                GL11.glEndList();
                if (!compiled) {
                    geometry.clear();
                    eligible = false;
                }
            }
            if (!compiled) {
                if (translucentBlocks != null) {
                    translucentBlocks.clear();
                }
                return false;
            }
            geometry.capture(blocks, renderAllFaces, translucentBlocks);
        } else if (translucentBlocks != null) {
            translucentBlocks.addAll(geometry.translucentBlocks);
        }
        GL11.glCallList(geometry.displayList);
        return true;
    }

    protected boolean isEligible(GuidebookLevel level) {
        int count = level.getFilledBlocks()
            .size();
        if (level.getClass() != GuidebookLevel.class || count < MIN_BLOCK_COUNT
            || count > MAX_BLOCK_COUNT
            || !level.getTileEntities()
                .isEmpty()) {
            return false;
        }
        for (int[] position : level.getFilledBlocks()) {
            Block block = level.getBlock(position[0], position[1], position[2]);
            if (!isStaticBlock(block)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isStaticBlock(Block block) {
        return block != null && STATIC_BLOCKS.get(block) == block.getClass();
    }

    public void release(GuidebookLevel level) {
        if (owner.get() == level) {
            clear();
        }
    }

    public void clear() {
        for (PassGeometry geometry : passes) {
            geometry.clear();
        }
        owner.clear();
        eligible = false;
    }

    private static class PassGeometry {

        private int displayList;
        private int[] coordinates = new int[0];
        private final ArrayList<int[]> translucentBlocks = new ArrayList<>();
        private boolean renderAllFaces;

        private boolean matches(List<int[]> blocks, boolean allFaces) {
            return displayList != 0 && matchesCoordinates(blocks, allFaces);
        }

        private boolean matchesCoordinates(List<int[]> blocks, boolean allFaces) {
            if (renderAllFaces != allFaces || coordinates.length != blocks.size() * 3) {
                return false;
            }
            for (int index = 0; index < blocks.size(); index++) {
                int[] position = blocks.get(index);
                int offset = index * 3;
                if (coordinates[offset] != position[0] || coordinates[offset + 1] != position[1]
                    || coordinates[offset + 2] != position[2]) {
                    return false;
                }
            }
            return true;
        }

        private void capture(List<int[]> blocks, boolean allFaces, @Nullable List<int[]> translucent) {
            int count = Math.multiplyExact(blocks.size(), 3);
            if (coordinates.length != count) {
                coordinates = new int[count];
            }
            for (int index = 0; index < blocks.size(); index++) {
                int[] position = blocks.get(index);
                int offset = index * 3;
                coordinates[offset] = position[0];
                coordinates[offset + 1] = position[1];
                coordinates[offset + 2] = position[2];
            }
            renderAllFaces = allFaces;
            if (translucent != null) {
                translucentBlocks.addAll(translucent);
            }
        }

        private void clear() {
            deleteDisplayList();
            coordinates = new int[0];
            translucentBlocks.clear();
        }

        private void deleteDisplayList() {
            if (displayList != 0) {
                GLAllocation.deleteDisplayLists(displayList);
                displayList = 0;
            }
        }
    }
}
