package com.hfstudio.guidenh.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class GuideNhStructureTransfers {

    private final Map<TransferKey, Transfer> transfers = new HashMap<>();
    private long totalBytes;
    private static final int MAX_TRANSFERS = 32;
    private static final int MAX_PLAYER_TRANSFERS = 2;
    private static final long MAX_TOTAL_BYTES = 64L * 1024L * 1024L;

    public synchronized @Nullable String accept(UUID playerId, GuideNhStructureRequestMessage message) {
        expire();
        TransferKey key = new TransferKey(playerId, message.getAction(), message.getTransferId());
        Transfer transfer = transfers.get(key);
        int index = message.getChunkIndex();
        int count = message.getChunkCount();
        byte[] payload = message.getStructureBytes();
        if (index < 0 || index >= count
            || count > GuideNhStructureRequestMessage.MAX_CHUNKS_PER_STRUCTURE
            || payload == null
            || payload.length > GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET) {
            throw new IllegalArgumentException("Invalid structure transfer fragment");
        }
        long playerBytes = 0L;
        int playerTransfers = 0;
        for (var entry : transfers.entrySet()) {
            if (entry.getKey()
                .playerId()
                .equals(playerId)) {
                playerTransfers++;
                playerBytes += entry.getValue()
                    .assembler()
                    .getTotalBytes();
            }
        }
        if (transfer == null) {
            if (transfers.size() >= MAX_TRANSFERS || playerTransfers >= MAX_PLAYER_TRANSFERS) {
                throw new IllegalStateException("Too many active structure transfers");
            }
            transfer = new Transfer(
                new GuideNhStructureChunkAssembler(count),
                message.getX(),
                message.getY(),
                message.getZ());
            transfers.put(key, transfer);
        }
        if (transfer.x() != message.getX() || transfer.y() != message.getY()
            || transfer.z() != message.getZ()
            || count != transfer.assembler()
                .getChunkCount()) {
            remove(key);
            throw new IllegalArgumentException("Structure transfer metadata changed");
        }
        GuideNhStructureChunkAssembler assembler = transfer.assembler();
        int addedBytes = assembler.hasChunk(index) ? 0 : payload.length;
        if (addedBytes > MAX_TOTAL_BYTES - totalBytes
            || addedBytes > GuideNhCustomPayloadLimits.MAX_STRUCTURE_TRANSFER_BYTES - playerBytes) {
            remove(key);
            throw new IllegalStateException("Structure transfers exceed their memory budget");
        }
        int before = assembler.getTotalBytes();
        String completed = assembler.accept(message);
        totalBytes += assembler.getTotalBytes() - before;
        if (completed != null || assembler.isExpired()) remove(key);
        return completed;
    }

    public synchronized void expire() {
        Iterator<Entry<TransferKey, Transfer>> iterator = transfers.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue()
                .assembler()
                .isExpired()) {
                totalBytes -= entry.getValue()
                    .assembler()
                    .getTotalBytes();
                iterator.remove();
            }
        }
    }

    public synchronized void clear(UUID playerId) {
        var iterator = transfers.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey()
                .playerId()
                .equals(playerId)) {
                totalBytes -= entry.getValue()
                    .assembler()
                    .getTotalBytes();
                iterator.remove();
            }
        }
    }

    public synchronized void clear() {
        transfers.clear();
        totalBytes = 0L;
    }

    private void remove(TransferKey key) {
        Transfer transfer = transfers.remove(key);
        if (transfer != null) totalBytes -= transfer.assembler()
            .getTotalBytes();
    }

    public record TransferKey(UUID playerId, byte action, int transferId) {}

    public record Transfer(GuideNhStructureChunkAssembler assembler, int x, int y, int z) {}
}
