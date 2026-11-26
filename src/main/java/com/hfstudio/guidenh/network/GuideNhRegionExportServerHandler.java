package com.hfstudio.guidenh.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.hfstudio.guidenh.guide.internal.item.RegionWandExporter;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExporter.IncrementalStructureExport;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhServerStructureAccess;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureMemoryStore;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhRegionExportServerHandler implements IMessageHandler<GuideNhRegionExportRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhRegionExportRequestMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player != null && !GuideNhServerRequests.instance()
            .enqueueWork(player, 128, new RegionExportWork(message))) {
            sendError(player, message.getRequestId(), "Server request queue is full");
        }
        return null;
    }

    private static void sendError(EntityPlayerMP player, int requestId, String error) {
        GuideNhNetwork.channel()
            .sendTo(GuideNhRegionExportReplyMessage.error(requestId, error), player);
    }

    public static class RegionExportWork implements GuideNhServerWork {

        private final GuideNhRegionExportRequestMessage message;
        private IncrementalStructureExport export;

        public RegionExportWork(GuideNhRegionExportRequestMessage message) {
            this.message = message;
        }

        @Override
        public boolean step(EntityPlayerMP player, long deadlineNanos) {
            if (!GuideNhServerStructureAccess.canUseSceneExport(player)
                || !GuideNhServerStructureAccess.isSameDimension(player, message.getDimensionId())) {
                sendError(player, message.getRequestId(), "Scene export is not allowed in this dimension");
                return true;
            }
            try {
                if (export == null) {
                    if (!validEnd(message.getX(), message.getSizeX()) || !validEnd(message.getY(), message.getSizeY())
                        || !validEnd(message.getZ(), message.getSizeZ())) {
                        sendError(player, message.getRequestId(), "Invalid region coordinates");
                        return true;
                    }
                    export = RegionWandExporter.createIncrementalSelectionExport(
                        player.worldObj,
                        message.getX(),
                        message.getY(),
                        message.getZ(),
                        message.getSizeX(),
                        message.getSizeY(),
                        message.getSizeZ(),
                        message.isIncludeEntities(),
                        false,
                        true);
                    if (export == null) {
                        sendError(player, message.getRequestId(), "Region too large");
                        return true;
                    }
                    export.setMaxRetainedBytes(GuideStructureMemoryStore.MAX_BYTES);
                }
                if (!export.step(deadlineNanos)) return false;
                for (GuideNhRegionExportReplyMessage packet : GuideNhRegionExportReplyMessage
                    .successPackets(message.getRequestId(), export.getResult()))
                    GuideNhNetwork.channel()
                        .sendTo(packet, player);
            } catch (RuntimeException error) {
                sendError(
                    player,
                    message.getRequestId(),
                    error.getMessage() != null ? error.getMessage() : "Region export failed");
            }
            return true;
        }

        private static boolean validEnd(int start, int size) {
            return size > 0 && (long) start + size - 1 <= Integer.MAX_VALUE;
        }

        @Override
        public void close() {
            if (export != null) export.cancel();
        }
    }
}
