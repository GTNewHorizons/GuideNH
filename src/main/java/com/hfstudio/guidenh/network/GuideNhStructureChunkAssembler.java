package com.hfstudio.guidenh.network;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.Nullable;

public class GuideNhStructureChunkAssembler {

    private final byte[][] chunks;
    private final long expiresAtNanos;
    private boolean discarded;
    private int received;
    private int totalBytes;

    public synchronized int getTotalBytes() {
        return totalBytes;
    }

    public int getChunkCount() {
        return chunks.length;
    }

    public synchronized boolean hasChunk(int index) {
        return chunks[index] != null;
    }

    public GuideNhStructureChunkAssembler(int chunkCount) {
        if (chunkCount <= 0 || chunkCount > GuideNhStructureRequestMessage.MAX_CHUNKS_PER_STRUCTURE) {
            throw new IllegalArgumentException("Invalid structure chunk count: " + chunkCount);
        }
        this.chunks = new byte[chunkCount][];
        this.expiresAtNanos = System.nanoTime() + GuideNhCustomPayloadLimits.STRUCTURE_TRANSFER_TTL_MILLIS * 1_000_000L;
    }

    @Nullable
    public synchronized String accept(GuideNhStructureRequestMessage message) {
        if (isExpired()) {
            return null;
        }
        int index = message.getChunkIndex();
        if (message.getChunkCount() != chunks.length || index < 0 || index >= chunks.length) {
            return null;
        }
        byte[] bytes = message.getStructureBytes();
        if (bytes == null || bytes.length > GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET) {
            return null;
        }
        if (chunks[index] == null) {
            chunks[index] = bytes;
            received++;
            totalBytes += bytes.length;
            if (totalBytes > GuideNhCustomPayloadLimits.MAX_STRUCTURE_TRANSFER_BYTES) {
                discarded = true;
                return null;
            }
        }
        if (received != chunks.length) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(totalBytes);
        for (byte[] chunk : chunks) {
            if (chunk == null) {
                return null;
            }
            out.write(chunk, 0, chunk.length);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    public boolean isExpired() {
        return discarded || System.nanoTime() > expiresAtNanos;
    }
}
