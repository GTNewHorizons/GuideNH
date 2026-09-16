package com.hfstudio.guidenh.integration.forgemultipart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator$;
import codechicken.multipart.BlockMultipart;
import codechicken.multipart.MultipartGenerator$;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.MultipartRenderer;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import cpw.mods.fml.common.Optional;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.collection.mutable.ListBuffer;

/** Direct ForgeMultipart integration for scene preview, exports, and block statistics. */
public final class ForgeMultipartHelpers {

    public static final String SAVED_MULTIPART_ID = "savedMultipart";

    public static final ConcurrentMap<String, Boolean> ONCE_KEYS = new ConcurrentHashMap<>();

    /** Lazily maps legacy unlocalized block IDs to blocks while reading old microblock material IDs. */
    public static volatile Map<String, Block> unlocalizedBlockCache;

    public ForgeMultipartHelpers() {}

    @Optional.Method(modid = "ForgeMultipart")
    public static boolean isForgeMultipartBlock(@Nullable Block block) {
        return block instanceof BlockMultipart;
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static boolean isMultipartTileEntity(@Nullable TileEntity tileEntity) {
        return tileEntity instanceof TileMultipart;
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static boolean isClientMultipartTileEntity(@Nullable TileEntity tileEntity) {
        return tileEntity instanceof TileMultipartClient;
    }

    public static boolean isSavedMultipartTag(@Nullable NBTTagCompound tag) {
        return tag != null && SAVED_MULTIPART_ID.equals(tag.getString("id"));
    }

    @Optional.Method(modid = "ForgeMultipart")
    @Nullable
    public static TileEntity loadPreviewTile(World world, Block block, int meta, int x, int y, int z,
        @Nullable NBTTagCompound tag) {
        if (world == null || tag == null || (!isForgeMultipartBlock(block) && !isSavedMultipartTag(tag))) {
            return null;
        }

        preFillMicroblockMaterials(tag);
        TileEntity tile = createMultipartTileFromNbt(world, withWorldPosition(tag, x, y, z));
        return tile == null || !world.isRemote ? tile : promoteClientMultipartTile(tile);
    }

    @Optional.Method(modid = "ForgeMultipart")
    @Nullable
    public static TileEntity finalizePreviewTile(@Nullable TileEntity tileEntity) {
        if (!(tileEntity instanceof TileMultipart)) {
            return tileEntity;
        }

        TileEntity finalized = ensureClientMultipartTile(tileEntity);
        if (!(finalized instanceof TileMultipart multipart)) {
            return finalized;
        }

        Seq<TMultiPart> parts = promotePartListToClientVariants(multipart.partList());
        multipart.loadParts(parts);
        multipart.notifyTileChange();
        multipart.markRender();
        if (!(multipart instanceof TileMultipartClient)) {
            warnOnce(
                "finalize-not-client:" + multipart.getClass()
                    .getName(),
                "[GuideNH] [ForgeMultipart] Multipart preview tile is not TileMultipartClient after finalization: {}",
                multipart.getClass()
                    .getName());
        }
        return multipart;
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static boolean renderWorldBlock(@Nullable RenderBlocks renderBlocks, @Nullable IBlockAccess blockAccess,
        @Nullable Block block, int x, int y, int z) {
        return renderBlocks != null && blockAccess != null
            && block instanceof BlockMultipart
            && MultipartRenderer.renderWorldBlock(blockAccess, x, y, z, block, block.getRenderType(), renderBlocks);
    }

    @Optional.Method(modid = "ForgeMultipart")
    @Nullable
    public static TileEntity createMultipartTileFromNbt(World world, NBTTagCompound tag) {
        return MultipartHelper.createTileFromNBT(world, tag);
    }

    /**
     * Rebuilds the parts using client microblock traits. FMP creates the client container trait independently, but
     * microblocks loaded from saved NBT still need their client rendering trait.
     */
    @Optional.Method(modid = "ForgeMultipart")
    public static Seq<TMultiPart> promotePartListToClientVariants(Seq<TMultiPart> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        ListBuffer<TMultiPart> rebuilt = new ListBuffer<>();
        boolean changed = false;
        Iterator<TMultiPart> iterator = source.iterator();
        while (iterator.hasNext()) {
            TMultiPart part = iterator.next();
            TMultiPart replacement = promoteMicroblockToClient(part);
            if (replacement != part) {
                changed = true;
            }
            rebuilt.$plus$eq(replacement);
        }
        return changed ? rebuilt.toSeq() : source;
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static TMultiPart promoteMicroblockToClient(TMultiPart part) {
        if (!(part instanceof Microblock microblock)) {
            return part;
        }

        Microblock replacement;
        try {
            replacement = MicroblockGenerator$.MODULE$.create(microblock.microClass(), microblock.material(), true);
        } catch (RuntimeException exception) {
            warnOnce(
                "promote-microblock-failed:" + microblock.getClass()
                    .getName(),
                "[GuideNH] [ForgeMultipart] Cannot prepare ForgeMultipart microblock {} for client rendering: {}",
                microblock.getClass()
                    .getName(),
                exception.toString());
            return part;
        }
        if (replacement == null) {
            warnOnce(
                "promote-microblock-null:" + microblock.getClass()
                    .getName(),
                "[GuideNH] [ForgeMultipart] MicroblockGenerator returned null while preparing {} for client rendering",
                microblock.getClass()
                    .getName());
            return part;
        }
        replacement.shape_$eq(microblock.shape());
        return replacement;
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static TileEntity promoteClientMultipartTile(TileEntity tileEntity) {
        if (!(tileEntity instanceof TileMultipart source) || tileEntity instanceof TileMultipartClient) {
            return tileEntity;
        }

        Seq<TMultiPart> parts = promotePartListToClientVariants(source.partList());
        if (parts == null || parts.isEmpty()) {
            warnOnce(
                "promote-no-parts:" + source.getClass()
                    .getName(),
                "[GuideNH] [ForgeMultipart] Cannot promote multipart preview tile {} because it has no parts",
                source.getClass()
                    .getName());
            return source;
        }

        TileMultipart promoted = MultipartGenerator$.MODULE$.generateCompositeTile(source, parts, true);
        if (promoted == source) {
            warnOnce(
                "promote-same-instance:" + source.getClass()
                    .getName(),
                "[GuideNH] [ForgeMultipart] MultipartGenerator did not add client traits to preview tile {}",
                source.getClass()
                    .getName());
            return source;
        }

        promoted.xCoord = source.xCoord;
        promoted.yCoord = source.yCoord;
        promoted.zCoord = source.zCoord;
        promoted.blockType = source.blockType;
        promoted.blockMetadata = source.blockMetadata;
        if (source.getWorldObj() != null) {
            promoted.setWorldObj(source.getWorldObj());
        }
        promoted.loadParts(parts);
        promoted.notifyTileChange();
        promoted.markRender();
        return promoted;
    }

    @Optional.Method(modid = "ForgeMultipart")
    @Nullable
    public static TileEntity ensureClientMultipartTile(@Nullable TileEntity tileEntity) {
        if (!Mods.ForgeMultipart.isModLoaded() || !(tileEntity instanceof TileMultipart)
            || tileEntity instanceof TileMultipartClient) {
            return tileEntity;
        }
        return promoteClientMultipartTile(tileEntity);
    }

    @Nullable
    public static NBTTagCompound snapshotTile(@Nullable TileEntity tileEntity) {
        if (tileEntity == null) {
            return null;
        }
        NBTTagCompound snapshot = new NBTTagCompound();
        tileEntity.writeToNBT(snapshot);
        return snapshot;
    }

    public static NBTTagCompound withWorldPosition(NBTTagCompound original, int x, int y, int z) {
        NBTTagCompound copy = (NBTTagCompound) original.copy();
        copy.setInteger("x", x);
        copy.setInteger("y", y);
        copy.setInteger("z", z);
        return copy;
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static void preFillMicroblockMaterials(@Nullable NBTTagCompound tag) {
        if (tag != null && Mods.ForgeMultipart.isModLoaded()) {
            scanNbtForMaterialIds(tag, new HashSet<>());
        }
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static void scanNbtForMaterialIds(NBTTagCompound tag, Set<String> seen) {
        for (String key : tag.func_150296_c()) {
            NBTBase value = tag.getTag(key);
            if (value instanceof NBTTagString stringTag) {
                String materialId = stringTag.func_150285_a_();
                if (looksLikeMicroblockMaterialId(materialId) && seen.add(materialId)) {
                    ensureMicroblockMaterialRegistered(materialId);
                }
            } else if (value instanceof NBTTagCompound compound) {
                scanNbtForMaterialIds(compound, seen);
            } else if (value instanceof NBTTagList list) {
                scanNbtListForMaterialIds(list, seen);
            }
        }
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static void scanNbtListForMaterialIds(NBTTagList list, Set<String> seen) {
        int type = list.func_150303_d();
        for (int i = 0; i < list.tagCount(); i++) {
            if (type == 8) {
                String materialId = list.getStringTagAt(i);
                if (looksLikeMicroblockMaterialId(materialId) && seen.add(materialId)) {
                    ensureMicroblockMaterialRegistered(materialId);
                }
            } else if (type == 10) {
                scanNbtForMaterialIds(list.getCompoundTagAt(i), seen);
            }
        }
    }

    public static boolean looksLikeMicroblockMaterialId(@Nullable String value) {
        if (value == null) {
            return false;
        }
        if (value.startsWith("tile.") || value.startsWith("item.")) {
            return true;
        }
        int colon = value.indexOf(':');
        return colon > 0 && colon < value.length() - 1
            && value.indexOf('/', colon) < 0
            && value.indexOf(' ') < 0
            && value.indexOf(':', colon + 1) < 0;
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static void ensureMicroblockMaterialRegistered(String materialId) {
        if (MicroMaterialRegistry.getMaterial(materialId) != null) {
            return;
        }

        MaterialTarget target = resolveMaterialTarget(materialId);
        if (target == null) {
            warnOnce(
                "fmp-material-no-block:" + materialId,
                "[GuideNH] [ForgeMultipart] Cannot prepare ForgeMultipart microblock material '{}': no matching block exists",
                materialId);
            return;
        }

        try {
            MicroMaterialRegistry.registerMaterial(new BlockMicroMaterial(target.block, target.meta), materialId);
            infoOnce(
                "fmp-material-registered:" + materialId,
                "[GuideNH] [ForgeMultipart] Registered ForgeMultipart microblock material {} (block={}, meta={})",
                materialId,
                target.block.getUnlocalizedName(),
                target.meta);
        } catch (IllegalStateException exception) {
            // FMP deliberately freezes this registry after its initialization phase. Unknown late additions must not
            // prevent the rest of a preview from loading.
            warnOnce(
                "fmp-material-registry-locked:" + materialId,
                "[GuideNH] [ForgeMultipart] ForgeMultipart material '{}' is unavailable because its registry is already initialized",
                materialId);
        }
    }

    @Nullable
    public static MaterialTarget resolveMaterialTarget(String materialId) {
        int meta;
        Block block;
        if (materialId.startsWith("tile.") || materialId.startsWith("item.")) {
            String withoutPrefix = materialId.substring(5);
            String baseName = stripMetaSuffix(withoutPrefix);
            meta = parseMetaSuffix(withoutPrefix);
            block = (Block) Block.blockRegistry.getObject(withoutPrefix);
            if (isAirOrNull(block)) {
                block = (Block) Block.blockRegistry.getObject(baseName);
            }
            if (isAirOrNull(block)) {
                Map<String, Block> cache = getUnlocalizedBlockCache();
                block = cache.get(materialId);
                if (isAirOrNull(block)) {
                    block = cache.get(materialId.substring(0, 5) + baseName);
                }
            }
        } else {
            int colon = materialId.indexOf(':');
            if (colon <= 0) {
                return null;
            }
            String name = materialId.substring(colon + 1);
            String baseName = stripMetaSuffix(name);
            meta = parseMetaSuffix(name);
            block = (Block) Block.blockRegistry.getObject(materialId.substring(0, colon + 1) + baseName);
            if (isAirOrNull(block)) {
                block = (Block) Block.blockRegistry.getObject(materialId);
            }
        }
        return isAirOrNull(block) ? null : new MaterialTarget(block, meta);
    }

    public static String stripMetaSuffix(String value) {
        int separator = value.lastIndexOf('_');
        return separator > 0 && isDigits(value.substring(separator + 1)) ? value.substring(0, separator) : value;
    }

    public static int parseMetaSuffix(String value) {
        int separator = value.lastIndexOf('_');
        return separator > 0 && isDigits(value.substring(separator + 1))
            ? Integer.parseInt(value.substring(separator + 1))
            : 0;
    }

    public static boolean isDigits(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAirOrNull(@Nullable Block block) {
        return block == null || block == Blocks.air;
    }

    public static Map<String, Block> getUnlocalizedBlockCache() {
        Map<String, Block> cache = unlocalizedBlockCache;
        if (cache != null) {
            return cache;
        }
        synchronized (ForgeMultipartHelpers.class) {
            if (unlocalizedBlockCache == null) {
                Map<String, Block> populated = new HashMap<>();
                for (Object value : Block.blockRegistry) {
                    if (value instanceof Block block) {
                        String name = block.getUnlocalizedName();
                        if (name != null && !name.isEmpty()) {
                            populated.putIfAbsent(name, block);
                        }
                    }
                }
                unlocalizedBlockCache = Map.copyOf(populated);
            }
            return unlocalizedBlockCache;
        }
    }

    @Optional.Method(modid = "ForgeMultipart")
    public static void appendMultipartStatStacks(@Nullable TileEntity tileEntity, List<ItemStack> output) {
        if (!(tileEntity instanceof TileMultipart multipart) || output == null || !Mods.ForgeMultipart.isModLoaded()) {
            return;
        }
        for (TMultiPart part : multipart.jPartList()) {
            if (part == null) {
                continue;
            }
            for (ItemStack stack : part.getDrops()) {
                if (stack != null && stack.getItem() != null) {
                    output.add(stack.copy());
                }
            }
        }
    }

    @Optional.Method(modid = "ForgeMultipart")
    @Nullable
    public static String resolvePrimaryMicroblockId(@Nullable TileEntity tileEntity) {
        if (!(tileEntity instanceof TileMultipart multipart)) {
            return null;
        }
        for (TMultiPart part : multipart.jPartList()) {
            if (!(part instanceof Microblock microblock)) {
                continue;
            }
            BlockMicroMaterial material;
            try {
                if (!(MicroMaterialRegistry
                    .getMaterial(microblock.material()) instanceof BlockMicroMaterial resolved)) {
                    continue;
                }
                material = resolved;
            } catch (RuntimeException exception) {
                warnOnce(
                    "resolve-microblock-material:" + microblock.getClass()
                        .getName(),
                    "[GuideNH] [ForgeMultipart] Cannot resolve ForgeMultipart microblock material for {}: {}",
                    microblock.getClass()
                        .getName(),
                    exception.toString());
                continue;
            }
            Block block = material.block();
            if (isAirOrNull(block)) {
                continue;
            }
            Object registryName = Block.blockRegistry.getNameForObject(block);
            if (registryName != null && !registryName.toString()
                .isEmpty()) {
                return material.meta() > 0 ? registryName + ":" + material.meta() : registryName.toString();
            }
        }
        return null;
    }

    public static void warnOnce(String key, String message, Object... args) {
        if (ONCE_KEYS.putIfAbsent(key, Boolean.TRUE) == null) {
            GuideDebugLog.warn(message, args);
        }
    }

    public static void infoOnce(String key, String message, Object... args) {
        if (ONCE_KEYS.putIfAbsent(key, Boolean.TRUE) == null) {
            GuideDebugLog.info(message, args);
        }
    }

    public record MaterialTarget(Block block, int meta) {}
}
