package com.hfstudio.guidenh.integration.ae2;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewWorld;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportSession;
import com.hfstudio.guidenh.guide.scene.snapshot.GuidebookLevelStructureExportAccess;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureExportAccess;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureExportPipeline;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockStatsStackResolver;

import appeng.api.AEApi;
import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.ICellCacheRegistry;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.parts.CableBusContainer;
import appeng.parts.networking.PartCable;
import appeng.tile.AEBaseInvTile;
import appeng.tile.AEBaseTile;
import appeng.tile.crafting.TileCraftingTile;
import appeng.tile.networking.TileCableBus;
import appeng.tile.qnb.TileQuantumBridge;
import appeng.tile.spatial.TileSpatialPylon;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;
import cpw.mods.fml.common.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * AE2 guide preview: applies server-authoritative AE2 preview bytes from {@link GuidebookLevel#previewAuthorityStore()}
 * ({@link Ae2ServerPreviewRegistration#SUPPLEMENT_ID} cable bus; {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}
 * other {@link AEBaseTile}). Cable connection state comes from the exported stream whenever one is available.
 */
public class Ae2Helpers {

    /** Low six bits of PartCable stream {@code cs}: {@link ForgeDirection#VALID_DIRECTIONS} only. */
    public static final int CS_DIRECTION_MASK = 0x3F;
    public static final String CABLE_BUS_TILE_ID = "BlockCableBus";
    public static final String CABLE_BUS_BLOCK_ID = "appliedenergistics2:tile.BlockCableBus";

    public Ae2Helpers() {}

    /**
     * Whether {@link World#markBlockForUpdate} must not reapply
     * {@link TileEntity#getDescriptionPacket}
     * for this TE inside the guidebook preview world:
     * {@link #prepare(GuidebookLevel)}
     * already merged server-authoritative preview bytes ({@link Ae2ServerPreviewRegistration#SUPPLEMENT_ID} /
     * {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}). Vanilla description resync rebuilds payloads from an
     * inert preview grid / proxy and overrides that state.
     */
    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    public static Block resolvePonderBlock(@Nullable NBTTagCompound tileTag) {
        if (tileTag == null || !CABLE_BUS_TILE_ID.equals(tileTag.getString("id"))) {
            return null;
        }
        Block block = (Block) Block.blockRegistry.getObject(CABLE_BUS_BLOCK_ID);
        return block != null && block != Blocks.air ? block : null;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static Map<String, byte[]> capturePonderPreviewSupplements(GuidebookLevel level, int x, int y, int z,
        @Nullable Block block, int meta) {
        if (level == null || block == null || block == Blocks.air) {
            return Map.of();
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileCableBus)) {
            return Map.of();
        }
        NBTTagCompound structureBlockTag = new NBTTagCompound();
        structureBlockTag.setIntArray("pos", new int[] { x, y, z });
        StructureExportAccess access = new GuidebookLevelStructureExportAccess(level);
        ExportSession session = new ExportSession(access, x, y, z, x, y, z, 1, 1, 1);
        StructureExportPipeline.beginExport(session);
        try {
            StructureExportPipeline
                .contributeBlock(new ExportBlockContext(session, x, y, z, block, meta, tileEntity, structureBlockTag));
        } finally {
            StructureExportPipeline.endExport(session);
        }
        return readPreviewSupplements(structureBlockTag);
    }

    public static Map<String, byte[]> readPreviewSupplements(@Nullable NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(ServerPreviewSupplementNbt.TAG_ROOT, 10)) {
            return Map.of();
        }
        NBTTagCompound root = tag.getCompoundTag(ServerPreviewSupplementNbt.TAG_ROOT);
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (String supplementId : root.func_150296_c()) {
            byte[] payload = ServerPreviewSupplementNbt.readSupplement(tag, supplementId);
            if (payload != null && payload.length > 0) {
                result.put(supplementId, payload);
            }
        }
        return result.isEmpty() ? Map.of() : result;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static boolean suppressMarkBlockForUpdateDescriptionResync(@Nullable TileEntity te, GuidebookLevel level) {
        if (te == null || level == null) {
            return false;
        }
        if (te instanceof TileCableBus) {
            return true;
        }
        // Chest/drive inventory NBT loading marks the fake world for update. Replaying
        // the unpatched description packet at that point clears the locally derived
        // cell type, status, and powered bit before the preview is rendered.
        if (te instanceof IChestOrDrive) {
            return true;
        }
        if (te instanceof AEBaseTile) {
            long posKey = GuidebookLevel.packPos(te.xCoord, te.yCoord, te.zCoord);
            byte[] blob = level.previewAuthorityStore()
                .get(posKey, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
            return blob != null && blob.length > 0;
        }
        return false;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (te instanceof TileCraftingTile craftingTile) {
                initCraftingTileValidSides(craftingTile);
            } else if (te instanceof TileQuantumBridge qnb) {
                applyNonCableBaseTilePreview(qnb, level);
                initQuantumBridgeValidSides(qnb);
            } else if (te instanceof AEBaseTile aeTile && !(te instanceof TileCableBus)) {
                initProxyOrientedValidSides(aeTile);
                applyNonCableBaseTilePreview(aeTile, level);
            }
        }
        for (TileEntity te : level.getTileEntities()) {
            CableBusContainer container = resolveCableContainer(te);
            if (container != null) {
                syncCableBusSidePartStreams(container, level);
            }
        }
        for (TileEntity te : level.getTileEntities()) {
            CableBusContainer container = resolveCableContainer(te);
            if (container != null && !hasCableAuthoritySnapshot(container, level)) {
                container.updateConnections();
            }
        }
        for (TileEntity te : level.getTileEntities()) {
            CableBusContainer container = resolveCableContainer(te);
            if (container != null) {
                syncCableBusConnections(container, level);
            }
        }
        if (level.getOrCreateFakeWorld() instanceof GuidebookPreviewWorld previewWorld) {
            previewWorld.syncLoadedTileEntities(level.getTileEntities());
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void initQuantumBridgeValidSides(TileQuantumBridge qnb) {
        if (!qnb.isFormed()) {
            return;
        }
        AENetworkProxy proxy;
        try {
            proxy = qnb.getProxy();
        } catch (Throwable ignored) {
            return;
        }
        if (proxy == null) {
            return;
        }
        if (qnb.isCorner() || isQuantumLinkCenter(qnb)) {
            try {
                proxy.setValidSides(qnb.getConnections());
            } catch (Throwable ignored) {}
        } else {
            proxy.setValidSides(EnumSet.allOf(ForgeDirection.class));
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static boolean isQuantumLinkCenter(TileQuantumBridge qnb) {
        Block link = (Block) Block.blockRegistry.getObject("appliedenergistics2:tile.BlockQuantumLinkChamber");
        return link != null && qnb.getBlockType() == link;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void initCraftingTileValidSides(TileCraftingTile craftingTile) {
        try {
            craftingTile.updateMeta(true);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void initProxyOrientedValidSides(AEBaseTile aeTile) {
        if (!aeTile.canBeRotated() || aeTile.getForward() == ForgeDirection.UNKNOWN) {
            return;
        }
        if (!(aeTile instanceof IGridProxyable proxyable)) {
            return;
        }
        AENetworkProxy proxy;
        try {
            proxy = proxyable.getProxy();
        } catch (Throwable ignored) {
            return;
        }
        if (proxy == null || !proxy.getConnectableSides()
            .isEmpty()) {
            return;
        }
        try {
            aeTile.setOrientation(aeTile.getForward(), aeTile.getUp());
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusConnections(TileCableBus cableBusTile, GuidebookLevel level) {
        syncCableBusConnections(cableBusTile.getCableBus(), level);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusConnections(CableBusContainer container, GuidebookLevel level) {
        if (!(container.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable)) {
            return;
        }

        TileEntity tile = container.getTile();
        long posKey = GuidebookLevel.packPos(tile.xCoord, tile.yCoord, tile.zCoord);
        byte[] raw = level.previewAuthorityStore()
            .get(posKey, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
        Ae2CablePreviewSnapshot snap = raw != null ? Ae2CablePreviewWireCodec.decode(raw)
            : Ae2CablePreviewSnapshot.EMPTY;

        int csOut;
        int sideOut;
        if (snap.hasCableCore()) {
            // The exported cable stream is authoritative. A selected single cable may no longer
            // have its original neighbours in the preview level, so recomputing these bits would
            // erase valid connections that were present when the structure was captured.
            csOut = snap.gridCsUnsigned();
            sideOut = snap.sideOut();
        } else {
            csOut = computeCableConnectionMask(container, level);
            sideOut = 0;
        }

        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte((byte) csOut);
        buf.writeInt(sideOut);
        try {
            cable.readFromStream(buf);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusSidePartStreams(TileCableBus cableBusTile, GuidebookLevel level) {
        syncCableBusSidePartStreams(cableBusTile.getCableBus(), level);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusSidePartStreams(CableBusContainer container, GuidebookLevel level) {
        TileEntity tile = container.getTile();
        long posKey = GuidebookLevel.packPos(tile.xCoord, tile.yCoord, tile.zCoord);
        byte[] raw = level.previewAuthorityStore()
            .get(posKey, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
        if (raw == null || raw.length == 0) {
            return;
        }
        Ae2CablePreviewSnapshot snap = Ae2CablePreviewWireCodec.decode(raw);
        if (snap.sideStreams()
            .isEmpty()) {
            return;
        }

        NBTTagCompound baseline = new NBTTagCompound();
        container.writeToNBT(baseline);

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            byte[] blob = snap.sideStreams()
                .bytesForSideOrdinal(dir.ordinal());
            if (blob == null || blob.length == 0) {
                continue;
            }
            if (container.getPart(dir) == null) {
                continue;
            }

            container.readFromNBT((NBTTagCompound) baseline.copy());
            tile.validate();
            IPart part = container.getPart(dir);
            if (part == null) {
                continue;
            }
            ByteBuf buf = Unpooled.wrappedBuffer(blob);
            try {
                part.readFromStream(buf);
                if (buf.readableBytes() == 0) {
                    container.writeToNBT(baseline);
                } else {
                    container.readFromNBT((NBTTagCompound) baseline.copy());
                    tile.validate();
                }
            } catch (Throwable ignored) {
                container.readFromNBT((NBTTagCompound) baseline.copy());
                tile.validate();
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static boolean hasCableAuthoritySnapshot(CableBusContainer container, GuidebookLevel level) {
        TileEntity tile = container.getTile();
        long posKey = GuidebookLevel.packPos(tile.xCoord, tile.yCoord, tile.zCoord);
        byte[] raw = level.previewAuthorityStore()
            .get(posKey, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
        return raw != null && Ae2CablePreviewWireCodec.decode(raw)
            .hasCableCore();
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void appendCableBusStatStacks(@Nullable TileEntity tileEntity, List<ItemStack> output) {
        if (!(tileEntity instanceof TileCableBus cableBusTile) || output == null) {
            return;
        }
        for (ForgeDirection direction : ForgeDirection.values()) {
            appendPartStatStack(cableBusTile.getPart(direction), output);
            if (direction != ForgeDirection.UNKNOWN) {
                appendFacadeStatStack(cableBusTile, direction, output);
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void appendCableBusStatEntries(@Nullable TileEntity tileEntity,
        List<GuideBlockStatsStackResolver.ResolvedStack> output, int x, int y, int z) {
        if (!(tileEntity instanceof TileCableBus cableBusTile) || output == null) {
            return;
        }
        for (ForgeDirection direction : ForgeDirection.values()) {
            appendPartStatEntry(cableBusTile.getPart(direction), output, x, y, z, direction);
            if (direction != ForgeDirection.UNKNOWN) {
                appendFacadeStatEntry(cableBusTile, direction, output, x, y, z);
            }
        }
    }

    public static void appendPartStatStack(@Nullable IPart part, List<ItemStack> output) {
        if (part == null) {
            return;
        }
        ItemStack stack = safePartStack(part, PartItemStack.Break);
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.World);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Pick);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Network);
        }
        appendCopy(output, stack);
    }

    public static void appendPartStatEntry(@Nullable IPart part,
        List<GuideBlockStatsStackResolver.ResolvedStack> output, int x, int y, int z, ForgeDirection direction) {
        if (part == null) {
            return;
        }
        ItemStack stack = safePartStack(part, PartItemStack.Break);
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.World);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Pick);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Network);
        }
        appendEntryCopy(output, stack, approximateCableBusBounds(x, y, z, direction, false));
    }

    @Nullable
    public static ItemStack safePartStack(IPart part, PartItemStack type) {
        try {
            ItemStack stack = part.getItemStack(type);
            return stack != null ? stack.copy() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void appendFacadeStatStack(TileCableBus cableBusTile, ForgeDirection direction,
        List<ItemStack> output) {
        try {
            IFacadePart facade = cableBusTile.getFacadeContainer()
                .getFacade(direction);
            if (facade != null) {
                appendCopy(output, facade.getItemStack());
            }
        } catch (Throwable ignored) {}
    }

    public static void appendFacadeStatEntry(TileCableBus cableBusTile, ForgeDirection direction,
        List<GuideBlockStatsStackResolver.ResolvedStack> output, int x, int y, int z) {
        try {
            IFacadePart facade = cableBusTile.getFacadeContainer()
                .getFacade(direction);
            if (facade != null) {
                appendEntryCopy(output, facade.getItemStack(), approximateCableBusBounds(x, y, z, direction, true));
            }
        } catch (Throwable ignored) {}
    }

    public static void appendCopy(List<ItemStack> output, @Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        output.add(stack.copy());
    }

    public static void appendEntryCopy(List<GuideBlockStatsStackResolver.ResolvedStack> output,
        @Nullable ItemStack stack, AxisAlignedBB bounds) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        output.add(new GuideBlockStatsStackResolver.ResolvedStack(stack.copy(), bounds));
    }

    public static AxisAlignedBB approximateCableBusBounds(int x, int y, int z, ForgeDirection direction,
        boolean facade) {
        double min = facade ? 0.0D : 0.25D;
        double max = facade ? 1.0D : 0.75D;
        double sideMin = facade ? 0.0D : 0.375D;
        double sideMax = facade ? 1.0D : 0.625D;
        double thickness = facade ? 0.125D : 0.25D;
        return switch (direction) {
            case DOWN -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y, z + sideMin, x + sideMax, y + thickness, z + sideMax);
            case UP -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y + 1.0D - thickness, z + sideMin, x + sideMax, y + 1.0D, z + sideMax);
            case NORTH -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y + sideMin, z, x + sideMax, y + sideMax, z + thickness);
            case SOUTH -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y + sideMin, z + 1.0D - thickness, x + sideMax, y + sideMax, z + 1.0D);
            case WEST -> AxisAlignedBB
                .getBoundingBox(x, y + sideMin, z + sideMin, x + thickness, y + sideMax, z + sideMax);
            case EAST -> AxisAlignedBB
                .getBoundingBox(x + 1.0D - thickness, y + sideMin, z + sideMin, x + 1.0D, y + sideMax, z + sideMax);
            default -> AxisAlignedBB.getBoundingBox(x + min, y + min, z + min, x + max, y + max, z + max);
        };
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static int computeCableConnectionMask(TileCableBus cableBusTile, GuidebookLevel level) {
        return computeCableConnectionMask(cableBusTile.getCableBus(), level);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static int computeCableConnectionMask(CableBusContainer container, GuidebookLevel level) {
        TileEntity tile = container.getTile();
        int x = tile.xCoord;
        int y = tile.yCoord;
        int z = tile.zCoord;

        if (!(container.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable)) {
            return 0;
        }
        AENetworkProxy sourceProxy = cable.getProxy();

        int cs = 0;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity adj = level.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
            AENetworkProxy targetProxy = resolveExternalConnectionProxy(adj, dir.getOpposite());
            if (targetProxy == null) {
                continue;
            }
            if (!Ae2CableConnectionRules.shouldConnect(sourceProxy, dir, targetProxy, dir.getOpposite())) {
                continue;
            }
            cs |= (1 << dir.ordinal());
        }
        return cs;
    }

    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    public static CableBusContainer resolveCableContainer(@Nullable TileEntity tileEntity) {
        return Ae2CableStructureSupport.resolveCableContainer(tileEntity);
    }

    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    public static AENetworkProxy resolveExternalConnectionProxy(@Nullable TileEntity tileEntity,
        ForgeDirection direction) {
        CableBusContainer container = resolveCableContainer(tileEntity);
        if (container != null) {
            IPart sidePart = container.getPart(direction);
            if (sidePart != null) {
                return sidePart instanceof Ae2ExternalGridPart externalPart
                    ? externalPart.guideNh$getExternalConnectionProxy()
                    : null;
            }
            IPart centerPart = container.getPart(ForgeDirection.UNKNOWN);
            return getProxy(centerPart);
        }
        return tileEntity instanceof IGridProxyable proxyable ? proxyable.getProxy() : null;
    }

    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    public static AENetworkProxy getProxy(@Nullable IPart part) {
        return part instanceof IGridProxyable proxyable ? proxyable.getProxy() : null;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void applyNonCableBaseTilePreview(AEBaseTile aeTile, GuidebookLevel level) {
        if (aeTile instanceof TileCableBus) {
            return;
        }
        long posKey = GuidebookLevel.packPos(aeTile.xCoord, aeTile.yCoord, aeTile.zCoord);
        byte[] blob = level.previewAuthorityStore()
            .get(posKey, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
        boolean applied = blob != null && blob.length > 0
            && Ae2BaseTileNetworkStreamPreview.applyAuthorityToPreviewTile(aeTile, blob);
        if (applied) {
            syncSpecialPreviewConnectableSides(aeTile);
            return;
        }
        // A hand-authored BlockImage has no server-side X supplement. Several AE2 tiles
        // keep their render state in transient fields (for example TileChest/TileDrive),
        // so a description packet produced before those fields are rebuilt would reset a
        // valid inventory from NBT to an empty-looking client state. Rebuild the tile's
        // derived state first, then mirror that state through the normal AE2 packet path.
        refreshLocalDerivedDisplayState(aeTile);
        syncDescriptionPacket(aeTile);
        syncSpecialPreviewConnectableSides(aeTile);
    }

    /** Rebuilds transient state only for storage tiles whose render data depends on cell handlers. */
    @Optional.Method(modid = "appliedenergistics2")
    public static void refreshLocalDerivedDisplayState(AEBaseTile tile) {
        if (!(tile instanceof TileChest || tile instanceof TileDrive)) {
            return;
        }
        try {
            tile.onReady();
        } catch (Throwable ignored) {
            // Continue with the ticking hook when available.
        }
        if (tile instanceof IGridTickable tickable) {
            try {
                // Storage tiles use this callback to rebuild cell caches and render flags.
                tickable.tickingRequest(null, 0);
            } catch (Throwable ignored) {
                // Some tiles require a live grid; leave their normal packet state untouched.
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncDescriptionPacket(AEBaseTile tile) {
        try {
            Packet packet = tile.getDescriptionPacket();
            if (packet instanceof S35PacketUpdateTileEntity updatePacket) {
                NBTTagCompound data = updatePacket.func_148857_g();
                if (data != null && data.hasKey("X", 7)) {
                    byte[] payload = data.getByteArray("X");
                    byte[] patched = patchLocalStorageDisplayState(tile, payload);
                    if (patched != null) {
                        data.setByteArray("X", patched);
                    }
                }
                tile.onDataPacket(null, updatePacket);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * AE2 storage renderers read state/type from client-only fields. A hand-authored BlockImage has no server stream,
     * so derive those two fields from the NBT-loaded cell and patch only the corresponding public packet payload.
     */
    @Optional.Method(modid = "appliedenergistics2")
    public static byte[] patchLocalStorageDisplayState(AEBaseTile tile, byte[] payload) {
        if (!(tile instanceof IChestOrDrive storage) || !(tile instanceof AEBaseInvTile inventory) || payload == null) {
            return payload;
        }

        if (tile instanceof TileChest chest) {
            if (payload.length < 3) return payload;
            ItemStack cell = inventory.getInternalInventory()
                .getStackInSlot(1);
            CellDisplay display = deriveCellDisplay(cell);
            int state = display.status() & 0b111;
            // The client renderer only sees this bit from the description stream.
            if (isActuallyPowered(chest)) {
                state |= 0b1000;
            }
            payload[1] = (byte) state;
            payload[2] = (byte) (display.type() & 0b11);
            return payload;
        }

        if (tile instanceof TileDrive) {
            if (payload.length < 9) return payload;
            int state = readInt(payload, 1) & 0x40000000;
            if (isActuallyPowered(tile)) {
                state |= 0x40000000;
            }
            int type = 0;
            int count = Math.min(
                storage.getCellCount(),
                inventory.getInternalInventory()
                    .getSizeInventory());
            for (int slot = 0; slot < count; slot++) {
                CellDisplay display = deriveCellDisplay(
                    inventory.getInternalInventory()
                        .getStackInSlot(slot));
                state |= (display.status() & 0b111) << (slot * 3);
                type |= (display.type() & 0b11) << (slot * 2);
            }
            writeInt(payload, 1, state);
            writeInt(payload, 5, type);
        }
        return payload;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static boolean isActuallyPowered(AEBaseTile tile) {
        if (tile instanceof IAEPowerStorage powerStorage) {
            if (powerStorage.getAECurrentPower() > 1.0D) {
                return true;
            }
        }
        if (!(tile instanceof IGridProxyable proxyable)) {
            return false;
        }
        try {
            AENetworkProxy proxy = proxyable.getProxy();
            return proxy != null && proxy.isActive()
                && proxy.getEnergy()
                    .isNetworkPowered();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static CellDisplay deriveCellDisplay(ItemStack cell) {
        if (cell == null) return CellDisplay.EMPTY;
        ICellHandler handler = AEApi.instance()
            .registries()
            .cell()
            .getHandler(cell);
        if (handler == null) return CellDisplay.EMPTY;
        for (IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
            try {
                IMEInventoryHandler inventory = handler.getCellInventory(cell, null, type);
                if (inventory != null) {
                    int status = Math.clamp(handler.getStatusForCell(cell, inventory), 0, 4);
                    return new CellDisplay(status, displayTypeFor(type, inventory));
                }
            } catch (Throwable ignored) {
                // A third-party cell may reject a channel while still supporting another one.
            }
        }
        return CellDisplay.EMPTY;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static int displayTypeFor(IAEStackType<?> target, IMEInventoryHandler inventory) {
        if (inventory instanceof ICellCacheRegistry cacheRegistry) {
            return Math.min(
                cacheRegistry.getCellType()
                    .ordinal(),
                2);
        }
        int index = 0;
        for (IAEStackType<?> type : AEStackTypeRegistry.getSortedTypes()) {
            if (type == target) {
                return Math.min(index, 2);
            }
            index++;
        }
        return 0;
    }

    public static void writeInt(byte[] payload, int offset, int value) {
        payload[offset] = (byte) (value >>> 24);
        payload[offset + 1] = (byte) (value >>> 16);
        payload[offset + 2] = (byte) (value >>> 8);
        payload[offset + 3] = (byte) value;
    }

    private static int readInt(byte[] payload, int offset) {
        return ((payload[offset] & 0xFF) << 24) | ((payload[offset + 1] & 0xFF) << 16)
            | ((payload[offset + 2] & 0xFF) << 8)
            | (payload[offset + 3] & 0xFF);
    }

    public record CellDisplay(int status, int type) {

        public static final CellDisplay EMPTY = new CellDisplay(0, 0);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncSpecialPreviewConnectableSides(AEBaseTile aeTile) {
        if (aeTile instanceof TileSpatialPylon spatialPylon) {
            syncSpatialPylonValidSides(spatialPylon);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncSpatialPylonValidSides(TileSpatialPylon spatialPylon) {
        EnumSet<ForgeDirection> validSides = hasSpatialPylonClusterState(spatialPylon)
            ? EnumSet.allOf(ForgeDirection.class)
            : EnumSet.noneOf(ForgeDirection.class);
        try {
            spatialPylon.getProxy()
                .setValidSides(validSides);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static boolean hasSpatialPylonClusterState(TileSpatialPylon spatialPylon) {
        return (spatialPylon.getDisplayBits() & TileSpatialPylon.MB_STATUS) != 0;
    }
}
