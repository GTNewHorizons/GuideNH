package com.hfstudio.guidenh.guide.internal.item;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureData;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureFileStore;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureVolume;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityImportSupport;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportSession;
import com.hfstudio.guidenh.guide.scene.snapshot.GuidebookLevelStructureExportAccess;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureExportAccess;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureExportPipeline;
import com.hfstudio.guidenh.guide.scene.snapshot.WorldStructureExportAccess;

/**
 * Serializes client-selected regions into GuideNH scene formats.
 */
public class RegionWandExporter {

    public static final int MAX_EXPORT_BLOCKS = 1_000_000;
    public static final double DEFAULT_REACH_DISTANCE = 5.0D;
    private static final long CLIENT_ACTION_DEBOUNCE_MILLIS = 180L;
    private static long lastClientSelectionActionMillis;
    @Nullable
    private static SelectionAction lastClientSelectionAction;
    private static int lastClientSelectionX;
    private static int lastClientSelectionY;
    private static int lastClientSelectionZ;
    private static long lastClientExportActionMillis;

    private RegionWandExporter() {}

    public static RegionWandExportMode getExportMode() {
        RegionWandExportMode mode = ModConfig.ui.regionWandExportMode;
        return mode != null ? mode : RegionWandExportMode.SNBT;
    }

    @Nullable
    public static String exportSelectionAsStructureSnbt(World world, boolean includeEntities) {
        if (world == null) {
            return null;
        }
        RegionWandSelection.Bounds bounds = RegionWandSelection.getBounds();
        if (bounds == null) {
            return null;
        }

        int minX = bounds.minX();
        int minY = bounds.minY();
        int minZ = bounds.minZ();
        int maxX = bounds.maxX();
        int maxY = bounds.maxY();
        int maxZ = bounds.maxZ();
        int dx = bounds.sizeX();
        int dy = bounds.sizeY();
        int dz = bounds.sizeZ();
        if (!includeEntities) {
            return exportRegionAsStructureSnbt(world, minX, minY, minZ, dx, dy, dz);
        }
        if (GuideStructureVolume.exceedsLimit(dx, dy, dz, MAX_EXPORT_BLOCKS)) {
            return null;
        }
        List<Entity> all = world.getEntitiesWithinAABBExcludingEntity(
            null,
            AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
        List<Entity> filtered = new ArrayList<>();
        for (Entity e : all) {
            if (!(e instanceof EntityPlayer) && EntityList.getEntityString(e) == null) continue;
            filtered.add(e);
        }
        return exportSnbt(world, minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz, filtered).text();
    }

    @Nullable
    public static String exportRegionAsStructureSnbt(World world, int minX, int minY, int minZ, int sizeX, int sizeY,
        int sizeZ) {
        if (world == null || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return null;
        }
        if (GuideStructureVolume.exceedsLimit(sizeX, sizeY, sizeZ, MAX_EXPORT_BLOCKS)) {
            return null;
        }
        int maxX = minX + sizeX - 1;
        int maxY = minY + sizeY - 1;
        int maxZ = minZ + sizeZ - 1;
        return exportSnbt(
            new WorldStructureExportAccess(world),
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            sizeX,
            sizeY,
            sizeZ,
            List.of()).text();
    }

    @Nullable
    public static GuideStructureData exportRegionAsStructureData(GuidebookLevel level, int minX, int minY, int minZ,
        int sizeX, int sizeY, int sizeZ) {
        if (level == null || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return null;
        }
        if (GuideStructureVolume.exceedsLimit(sizeX, sizeY, sizeZ, MAX_EXPORT_BLOCKS)) {
            return null;
        }
        int maxX = minX + sizeX - 1;
        int maxY = minY + sizeY - 1;
        int maxZ = minZ + sizeZ - 1;
        return exportStructure(
            new GuidebookLevelStructureExportAccess(level),
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            sizeX,
            sizeY,
            sizeZ).structure();
    }

    public static void exportToClipboard(EntityPlayer player, World world) {
        if (!ModConfig.ui.sceneExportEnabled) {
            if (world != null && world.isRemote) {
                send(player, GuidebookText.SceneExportDisabled);
            }
            return;
        }
        RegionWandSelection.Bounds bounds = RegionWandSelection.getBounds();
        if (bounds == null) {
            if (world.isRemote) {
                send(player, GuidebookText.RegionWandNeedTwoCorners);
            }
            return;
        }
        if (!world.isRemote) return;

        int minX = bounds.minX();
        int minY = bounds.minY();
        int minZ = bounds.minZ();
        int maxX = bounds.maxX();
        int maxY = bounds.maxY();
        int maxZ = bounds.maxZ();
        int dx = bounds.sizeX();
        int dy = bounds.sizeY();
        int dz = bounds.sizeZ();

        long total = GuideStructureVolume.blockCount(dx, dy, dz);
        if (total > MAX_EXPORT_BLOCKS) {
            send(player, GuidebookText.RegionWandAreaTooLarge, total);
            return;
        }

        RegionWandExportMode mode = getExportMode();
        List<Entity> entities = List.of();
        if (mode.includeEntities()) {
            List<Entity> all = world.getEntitiesWithinAABBExcludingEntity(
                null,
                AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
            List<Entity> filtered = new ArrayList<>();
            for (Entity e : all) {
                // Include players as FakePlayer preview entities; skip non-player entities with no registered ID
                if (!(e instanceof EntityPlayer) && EntityList.getEntityString(e) == null) continue;
                filtered.add(e);
            }
            entities = filtered;
        }
        boolean isBlocks = mode == RegionWandExportMode.BLOCKS || mode == RegionWandExportMode.BLOCKS_ENTITIES;
        ExportResult result;
        if (isBlocks) {
            result = exportBlocks(world, minX, minY, minZ, maxX, maxY, maxZ, entities);
        } else {
            result = exportSnbt(world, minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz, entities);
        }

        if (isBlocks) {
            try {
                Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(result.text), null);
                send(player, GuidebookText.RegionWandCopied, dx, dy, dz, result.nonAir, result.teCount);
                if (result.entityCount > 0) {
                    send(player, GuidebookText.RegionWandEntitiesExported, result.entityCount);
                }
            } catch (Throwable t) {
                send(player, GuidebookText.RegionWandCopyFailed, getErrorMessage(t));
            }
        } else {
            try {
                Path savedPath = GuideStructureFileStore.createDefault()
                    .saveExport("structure", result.text);
                send(
                    player,
                    GuidebookText.RegionWandSavedSnbt,
                    dx,
                    dy,
                    dz,
                    result.nonAir,
                    result.teCount,
                    savedPath.toAbsolutePath()
                        .normalize()
                        .toString());
                if (result.entityCount > 0) {
                    send(player, GuidebookText.RegionWandEntitiesExported, result.entityCount);
                }
            } catch (Throwable t) {
                send(player, GuidebookText.RegionWandCopyFailed, getErrorMessage(t));
            }
        }
    }

    public static ExportResult exportBlocks(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
        List<Entity> entities) {
        StringBuilder sb = new StringBuilder();
        sb.append("<GameScene zoom={4} interactive={true}>\n");
        int nonAir = 0;
        int teCount = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    Block block = world.getBlock(x, y, z);
                    if (block == null || block == Blocks.air) continue;
                    String regName = Block.blockRegistry.getNameForObject(block);
                    if (regName == null || regName.isEmpty()) continue;
                    int meta = world.getBlockMetadata(x, y, z);
                    int rx = x - minX;
                    int ry = y - minY;
                    int rz = z - minZ;

                    String nbtSnbt = null;
                    TileEntity te = world.getTileEntity(x, y, z);
                    if (te != null) {
                        try {
                            NBTTagCompound teTag = new NBTTagCompound();
                            te.writeToNBT(teTag);
                            teTag.removeTag("x");
                            teTag.removeTag("y");
                            teTag.removeTag("z");
                            String s = GuideTextNbtCodec.writeTextSafeCompound(teTag);
                            nbtSnbt = s.replace("'", "\\'");
                        } catch (Throwable t) {
                            nbtSnbt = null;
                        }
                    }

                    sb.append("    <Block id=\"")
                        .append(regName)
                        .append('"');
                    if (rx != 0) sb.append(" x=\"")
                        .append(rx)
                        .append('"');
                    if (ry != 0) sb.append(" y=\"")
                        .append(ry)
                        .append('"');
                    if (rz != 0) sb.append(" z=\"")
                        .append(rz)
                        .append('"');
                    if (meta != 0) sb.append(" meta=\"")
                        .append(meta)
                        .append('"');
                    if (nbtSnbt != null) {
                        sb.append(" nbt='")
                            .append(nbtSnbt)
                            .append('\'');
                        teCount++;
                    }
                    sb.append(" />\n");
                    nonAir++;
                }
            }
        }
        int entityCount = 0;
        for (Entity entity : entities) {
            String entityId;
            String playerName = null;
            String dataNbt = null;
            if (entity instanceof EntityPlayer ep) {
                entityId = "Player";
                playerName = ep.getGameProfile()
                    .getName();
            } else {
                entityId = EntityList.getEntityString(entity);
                if (entityId == null) continue;
                NBTTagCompound entityNbt = new NBTTagCompound();
                try {
                    entity.writeToNBT(entityNbt);
                } catch (Throwable ignored) {
                    continue;
                }
                entityNbt.removeTag("Pos");
                entityNbt.removeTag("Motion");
                entityNbt.removeTag("id");
                GuidebookSceneEntityImportSupport.sanitizeCustomName(entityNbt);
                if (!entityNbt.hasNoTags()) {
                    try {
                        String s = GuideTextNbtCodec.writeTextSafeCompound(entityNbt);
                        dataNbt = s.replace("'", "\\'");
                    } catch (Throwable ignored) {}
                }
            }
            float rx = (float) (entity.posX - minX);
            float ry = (float) (entity.posY - minY);
            float rz = (float) (entity.posZ - minZ);
            sb.append("    <Entity id=\"")
                .append(entityId)
                .append('"');
            if (playerName != null) {
                sb.append(" name=\"")
                    .append(playerName)
                    .append('"');
            }
            sb.append(" x=\"")
                .append(rx)
                .append('"')
                .append(" y=\"")
                .append(ry)
                .append('"')
                .append(" z=\"")
                .append(rz)
                .append('"');
            sb.append(" rotationY=\"")
                .append(entity.rotationYaw)
                .append('"')
                .append(" rotationX=\"")
                .append(entity.rotationPitch)
                .append('"');
            if (dataNbt != null && !dataNbt.isEmpty() && !"{}".equals(dataNbt)) {
                sb.append(" data='")
                    .append(dataNbt)
                    .append('\'');
            }
            sb.append(" />\n");
            entityCount++;
        }
        sb.append("</GameScene>\n");
        return new ExportResult(sb.toString(), nonAir, teCount, entityCount);
    }

    public static ExportResult exportSnbt(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
        int dx, int dy, int dz, List<Entity> entities) {
        return exportSnbt(
            new WorldStructureExportAccess(world),
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            dx,
            dy,
            dz,
            entities);
    }

    private static ExportResult exportSnbt(StructureExportAccess access, int minX, int minY, int minZ, int maxX,
        int maxY, int maxZ, int dx, int dy, int dz, List<Entity> entities) {
        ExportPayload payload = exportStructure(access, minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz, entities);
        return new ExportResult(
            GuideTextNbtCodec.writeStructureSnbt(
                payload.structure()
                    .getRoot()),
            payload.nonAir(),
            payload.teCount(),
            payload.entityCount());
    }

    private static ExportPayload exportStructure(StructureExportAccess access, int minX, int minY, int minZ, int maxX,
        int maxY, int maxZ, int dx, int dy, int dz) {
        return exportStructure(access, minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz, List.of());
    }

    private static ExportPayload exportStructure(StructureExportAccess access, int minX, int minY, int minZ, int maxX,
        int maxY, int maxZ, int dx, int dy, int dz, List<Entity> entities) {
        Map<String, Integer> paletteIndex = new HashMap<>();
        NBTTagList paletteList = new NBTTagList();
        NBTTagList blocksList = new NBTTagList();
        int nonAir = 0;
        int teCount = 0;
        ExportSession exportSession = new ExportSession(access, minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz);
        StructureExportPipeline.beginExport(exportSession);
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    Block block = access.getBlock(x, y, z);
                    if (block == null || block == Blocks.air) continue;
                    String regName = access.getBlockId(x, y, z, block);
                    if (regName == null || regName.isEmpty()) continue;
                    int meta = access.getBlockMetadata(x, y, z);

                    Integer idx = paletteIndex.get(regName);
                    if (idx == null) {
                        idx = paletteList.tagCount();
                        var entry = new NBTTagCompound();
                        entry.setString("Name", regName);
                        paletteList.appendTag(entry);
                        paletteIndex.put(regName, idx);
                    }

                    var blockTag = new NBTTagCompound();
                    blockTag.setIntArray("pos", new int[] { x - minX, y - minY, z - minZ });
                    blockTag.setInteger("state", idx);
                    if (meta != 0) blockTag.setInteger("meta", meta);

                    TileEntity te = access.getTileEntity(x, y, z);
                    if (te != null) {
                        try {
                            NBTTagCompound teTag = new NBTTagCompound();
                            te.writeToNBT(teTag);
                            teTag.removeTag("x");
                            teTag.removeTag("y");
                            teTag.removeTag("z");
                            blockTag.setTag("nbt", teTag);
                            teCount++;
                        } catch (Throwable ignored) {}
                    }
                    StructureExportPipeline
                        .contributeBlock(new ExportBlockContext(exportSession, x, y, z, block, meta, te, blockTag));
                    blocksList.appendTag(blockTag);
                    nonAir++;
                }
            }
        }
        StructureExportPipeline.endExport(exportSession);

        var root = new NBTTagCompound();
        root.setIntArray("size", new int[] { dx, dy, dz });
        root.setTag("palette", paletteList);
        root.setTag("blocks", blocksList);
        int entityCount = 0;
        if (!entities.isEmpty()) {
            NBTTagList entitiesList = new NBTTagList();
            for (Entity entity : entities) {
                String entityId;
                String playerName = null;
                if (entity instanceof EntityPlayer ep) {
                    entityId = "Player";
                    playerName = ep.getGameProfile()
                        .getName();
                } else {
                    entityId = EntityList.getEntityString(entity);
                    if (entityId == null) continue;
                }
                var entry = new NBTTagCompound();
                entry.setString("id", entityId);
                entry.setFloat("px", (float) (entity.posX - minX));
                entry.setFloat("py", (float) (entity.posY - minY));
                entry.setFloat("pz", (float) (entity.posZ - minZ));
                // Always store rotation at entry level to avoid NBTTagList<float> round-trip
                // issues with Minecraft 1.7.10's text NBT parser.
                entry.setFloat("yaw", entity.rotationYaw);
                entry.setFloat("pitch", entity.rotationPitch);
                if (entity instanceof EntityLivingBase living) {
                    entry.setFloat("bodyYaw", living.renderYawOffset);
                    entry.setFloat("headYaw", living.rotationYawHead);
                }
                if (playerName != null) {
                    entry.setString("name", playerName);
                }
                // Write NBT for ALL entities, including players (for held items / equipment).
                NBTTagCompound entityNbt = new NBTTagCompound();
                try {
                    entity.writeToNBT(entityNbt);
                } catch (Throwable ignored) {
                    if (playerName == null) continue; // non-player: skip on serialization error
                }
                entityNbt.removeTag("Pos");
                entityNbt.removeTag("Motion");
                entityNbt.removeTag("id");
                entityNbt.removeTag("Rotation"); // stored separately as yaw/pitch above
                GuidebookSceneEntityImportSupport.sanitizeCustomName(entityNbt);
                if (!entityNbt.hasNoTags()) {
                    entry.setTag("nbt", entityNbt);
                }
                entitiesList.appendTag(entry);
                entityCount++;
            }
            if (entitiesList.tagCount() > 0) {
                root.setTag("entities", entitiesList);
            }
        }
        return new ExportPayload(new GuideStructureData(root, dx, dy, dz), nonAir, teCount, entityCount);
    }

    @Desugar
    public record ExportResult(String text, int nonAir, int teCount, int entityCount) {}

    @Desugar
    private record ExportPayload(GuideStructureData structure, int nonAir, int teCount, int entityCount) {}

    public static void send(EntityPlayer player, GuidebookText key, Object... args) {
        player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    public static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }

    public static int[] floorBlockPosition(double x, double y, double z) {
        return new int[] { MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z) };
    }

    public static int @Nullable [] resolveLookingAtSelection(EntityPlayer player, World world,
        boolean selectBlockBody) {
        return resolveLookingAtSelection(player, world, selectBlockBody, DEFAULT_REACH_DISTANCE);
    }

    public static int @Nullable [] resolveLookingAtSelection(EntityPlayer player, World world, boolean selectBlockBody,
        double reachDistance) {
        if (player == null || world == null) {
            return null;
        }
        Vec3 start = getEyePosition(player, world);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start
            .addVector(look.xCoord * reachDistance, look.yCoord * reachDistance, look.zCoord * reachDistance);
        MovingObjectPosition hit = world.func_147447_a(start, end, false, true, false);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return selectionPositionFromHit(hit, selectBlockBody);
        }
        return floorBlockPosition(end.xCoord, end.yCoord, end.zCoord);
    }

    public static int[] selectionPositionFromHit(MovingObjectPosition hit, boolean selectBlockBody) {
        int x = hit.blockX;
        int y = hit.blockY;
        int z = hit.blockZ;
        if (!selectBlockBody) {
            ForgeDirection side = ForgeDirection.getOrientation(hit.sideHit);
            x += side.offsetX;
            y += side.offsetY;
            z += side.offsetZ;
        }
        return new int[] { x, y, z };
    }

    private static Vec3 getEyePosition(EntityPlayer player, World world) {
        float partialTicks = 1.0F;
        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks
            + (world.isRemote ? player.getEyeHeight() - player.getDefaultEyeHeight() : player.getEyeHeight());
        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        return Vec3.createVectorHelper(x, y, z);
    }

    public static void applySelectionAction(EntityPlayer player, SelectionAction action, int x, int y, int z) {
        applySelectionAction(player, action, x, y, z, true);
    }

    public static void applySelectionAction(EntityPlayer player, SelectionAction action, int x, int y, int z,
        boolean sendFeedback) {
        if (player == null || action == null) {
            return;
        }
        if (sendFeedback && player.worldObj != null
            && player.worldObj.isRemote
            && !beginClientSelectionAction(action, x, y, z)) {
            return;
        }
        switch (action) {
            case POS1:
                RegionWandSelection.setPos(1, x, y, z);
                if (sendFeedback) {
                    send(player, GuidebookText.RegionWandChatPos, 1, x, y, z);
                }
                break;
            case POS2:
                RegionWandSelection.setPos(2, x, y, z);
                if (sendFeedback) {
                    send(player, GuidebookText.RegionWandChatPos, 2, x, y, z);
                }
                break;
            case CLEAR:
                RegionWandSelection.clear();
                if (sendFeedback) {
                    send(player, GuidebookText.RegionWandSelectionCleared);
                }
                break;
            default:
                break;
        }
    }

    public static boolean beginClientSelectionAction(SelectionAction action, int x, int y, int z) {
        long now = System.currentTimeMillis();
        if (lastClientSelectionAction == action && lastClientSelectionX == x
            && lastClientSelectionY == y
            && lastClientSelectionZ == z
            && now - lastClientSelectionActionMillis < CLIENT_ACTION_DEBOUNCE_MILLIS) {
            return false;
        }
        lastClientSelectionAction = action;
        lastClientSelectionX = x;
        lastClientSelectionY = y;
        lastClientSelectionZ = z;
        lastClientSelectionActionMillis = now;
        return true;
    }

    public static boolean beginClientExportAction() {
        long now = System.currentTimeMillis();
        if (now - lastClientExportActionMillis < CLIENT_ACTION_DEBOUNCE_MILLIS) {
            return false;
        }
        lastClientExportActionMillis = now;
        return true;
    }

    public enum SelectionAction {

        POS1,
        POS2,
        CLEAR
    }
}
