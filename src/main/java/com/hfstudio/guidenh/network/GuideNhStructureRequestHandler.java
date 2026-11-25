package com.hfstudio.guidenh.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhServerStructureAccess;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureMemoryStore;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureWorldPlacementTarget;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhStructureRequestHandler implements IMessageHandler<GuideNhStructureRequestMessage, IMessage> {

    private static final GuideNhStructureTransfers CHUNK_TRANSFERS = new GuideNhStructureTransfers();

    @Override
    public IMessage onMessage(GuideNhStructureRequestMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        if (!GuideNhServerRequests.instance()
            .enqueue(player, message.getStructureBytes().length + 64, queuedPlayer -> handle(message, queuedPlayer))) {
            send(player, GuidebookText.CommandStructureImportFailure, "Server request queue is full");
        }
        return null;
    }

    private IMessage handle(GuideNhStructureRequestMessage message, EntityPlayerMP player) {
        CHUNK_TRANSFERS.expire();
        var playerId = player.getUniqueID();
        var sessionStore = GuideNhStructureRuntime.getServerSessionStore();

        try {
            byte action = message.getAction();
            String structureText = message.isChunkedStructureTransfer() ? null : message.getStructureText();
            if (message.isChunkedStructureTransfer()) {
                if (!GuideNhServerStructureAccess.canUseSceneExport(player)) {
                    send(player, GuidebookText.CommandStructurePermissionDenied);
                    return null;
                }
                structureText = CHUNK_TRANSFERS.accept(playerId, message);
                if (structureText == null) return null;
                action = message.getCompletedChunkAction();
            }

            switch (action) {
                case GuideNhStructureRequestMessage.ACTION_CACHE:
                    if (!GuideNhServerStructureAccess.canUseSceneExport(player)) {
                        send(player, GuidebookText.CommandStructurePermissionDenied);
                        break;
                    }
                    sessionStore.remember(playerId, "client-cache", structureText);
                    break;
                case GuideNhStructureRequestMessage.ACTION_IMPORT_AND_PLACE:
                    if (!GuideNhServerStructureAccess.hasStructurePermission(player)) {
                        send(player, GuidebookText.CommandStructurePermissionDenied);
                        break;
                    }
                    GuideStructureMemoryStore.Entry entry = sessionStore
                        .remember(playerId, "client-import", structureText);
                    GuideNhStructureRuntime.getPlacementService()
                        .place(
                            new GuideStructureWorldPlacementTarget(player.worldObj),
                            entry.getData(),
                            message.getX(),
                            message.getY(),
                            message.getZ());
                    send(
                        player,
                        GuidebookText.CommandStructureImportSuccess,
                        message.getX(),
                        message.getY(),
                        message.getZ());
                    break;
                case GuideNhStructureRequestMessage.ACTION_PLACE_ALL:
                    if (!GuideNhServerStructureAccess.hasStructurePermission(player)) {
                        send(player, GuidebookText.CommandStructurePermissionDenied);
                        break;
                    }
                    var structures = sessionStore.snapshotData(playerId);
                    if (structures.isEmpty()) {
                        send(player, GuidebookText.CommandStructureNoMemory);
                        break;
                    }
                    GuideNhStructureRuntime.getPlacementService()
                        .placeAll(
                            new GuideStructureWorldPlacementTarget(player.worldObj),
                            structures,
                            message.getX(),
                            message.getY(),
                            message.getZ());
                    send(
                        player,
                        GuidebookText.CommandStructurePlacedAll,
                        structures.size(),
                        message.getX(),
                        message.getY(),
                        message.getZ());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            send(player, GuidebookText.CommandStructureImportFailure, getErrorMessage(e));
        }
        return null;
    }

    public static void send(EntityPlayerMP player, GuidebookText key, Object... args) {
        player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    public static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }

    public static void expireTransfers() {
        CHUNK_TRANSFERS.expire();
    }

    public static void clearTransfers(UUID playerId) {
        CHUNK_TRANSFERS.clear(playerId);
    }

    public static void clearTransfers() {
        CHUNK_TRANSFERS.clear();
    }
}
