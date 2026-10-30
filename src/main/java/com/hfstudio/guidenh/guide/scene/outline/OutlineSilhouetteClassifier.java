package com.hfstudio.guidenh.guide.scene.outline;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWeb;
import net.minecraftforge.common.IPlantable;

/**
 * Picks the {@link OutlineSilhouette} that matches how a target is actually drawn.
 *
 * <p>
 * The rule of thumb is "does the geometry already describe the shape?". Closed models (cubes, panes,
 * multipart boxes, OBJ/TESR models, glass and fluids) do, so their rendered geometry is used and
 * transparent texels such as a glass frame's interior cannot punch holes into the outline. Sprite-like
 * renders (crossed plants, torches, rails, ladders, vines, redstone, entity skins) only show their shape
 * through texture alpha, so those keep the alpha-tested silhouette.
 */
public final class OutlineSilhouetteClassifier {

    private static final int[] SPRITE_RENDER_TYPE_IDS = { 1, 2, 3, 5, 6, 8, 9, 15, 19, 20, 23, 25, 29, 30, 33, 36, 40 };
    private static final boolean[] SPRITE_RENDER_TYPES = spriteRenderTypes();

    private OutlineSilhouetteClassifier() {}

    public static OutlineSilhouette forBlock(Block block, boolean hasSpecialRenderer, OutlineMode mode) {
        return switch (mode) {
            case GEOMETRY -> OutlineSilhouette.GEOMETRY;
            case CUTOUT -> OutlineSilhouette.CUTOUT;
            case BOUNDS -> OutlineSilhouette.BOUNDS;
            case AUTO -> classifyBlock(block, hasSpecialRenderer);
        };
    }

    public static OutlineSilhouette forEntity(OutlineMode mode) {
        return switch (mode) {
            case GEOMETRY -> OutlineSilhouette.GEOMETRY;
            case BOUNDS -> OutlineSilhouette.BOUNDS;
            case AUTO, CUTOUT -> OutlineSilhouette.CUTOUT;
        };
    }

    private static OutlineSilhouette classifyBlock(Block block, boolean hasSpecialRenderer) {
        int renderType = block.getRenderType();
        if (renderType < 0 && !hasSpecialRenderer) {
            return OutlineSilhouette.BOUNDS;
        }
        if (block.isOpaqueCube() || block.getRenderBlockPass() > 0) {
            return OutlineSilhouette.GEOMETRY;
        }
        return isSpriteLike(block, renderType) ? OutlineSilhouette.CUTOUT : OutlineSilhouette.GEOMETRY;
    }

    private static boolean isSpriteLike(Block block, int renderType) {
        if (renderType >= 0 && renderType < SPRITE_RENDER_TYPES.length && SPRITE_RENDER_TYPES[renderType]) {
            return true;
        }
        return block instanceof IPlantable || block instanceof BlockTorch
            || block instanceof BlockRailBase
            || block instanceof BlockLadder
            || block instanceof BlockVine
            || block instanceof BlockWeb
            || block instanceof BlockFire
            || block instanceof BlockRedstoneWire;
    }

    private static boolean[] spriteRenderTypes() {
        int max = 0;
        for (int renderType : SPRITE_RENDER_TYPE_IDS) {
            max = Math.max(max, renderType);
        }
        boolean[] table = new boolean[max + 1];
        for (int renderType : SPRITE_RENDER_TYPE_IDS) {
            table[renderType] = true;
        }
        return table;
    }
}
