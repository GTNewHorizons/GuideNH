package com.hfstudio.guidenh.client.capture.render;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryReservation;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.api.CaptureTile;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;

public class CaptureTileStore implements CaptureRows {

    private final Path path;
    private final FileChannel channel;
    private final CaptureOutputSize size;
    private final int background;
    private ByteBuffer buffer;
    private final CaptureMemoryAccount memory;
    private CaptureMemoryReservation bufferReservation;
    private boolean closed;

    public CaptureTileStore(Path directory, CaptureOutputSize size, int background, long diskBudget)
        throws IOException {
        this(directory, size, background, diskBudget, null);
    }

    public CaptureTileStore(Path directory, CaptureOutputSize size, int background, long diskBudget,
        CaptureMemoryAccount memory) throws IOException {
        this.memory = memory;
        if (size.rgbaBytes() > diskBudget) {
            throw new IllegalArgumentException("The high-resolution source exceeds the temporary disk budget");
        }
        Files.createDirectories(directory);
        path = Files.createTempFile(directory, ".guidenh_tiles_", ".rgba");
        FileChannel opened = null;
        try {
            opened = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
            opened.write(ByteBuffer.wrap(new byte[1]), size.rgbaBytes() - 1);
        } catch (IOException | RuntimeException | Error error) {
            CaptureCleanup cleanup = new CaptureCleanup(error);
            cleanup.close(opened);
            cleanup.run(() -> Files.deleteIfExists(path));
            throw error;
        }
        channel = opened;
        this.size = size;
        this.background = background;
    }

    @Override
    public CaptureOutputSize size() {
        return size;
    }

    private ByteBuffer buffer(int count) {
        if (closed) throw new IllegalStateException("The capture tile store is closed");
        if (buffer == null || buffer.capacity() < count) {
            releaseBuffer();
            CaptureMemoryReservation reservation = new CaptureMemoryReservation(memory, count);
            try {
                buffer = ByteBuffer.allocate(count)
                    .order(ByteOrder.LITTLE_ENDIAN);
                bufferReservation = reservation;
            } catch (RuntimeException | Error failure) {
                CaptureCleanup cleanup = new CaptureCleanup(failure);
                cleanup.close(reservation);
                throw failure;
            }
        }
        buffer.clear()
            .limit(count);
        return buffer;
    }

    public void put(CaptureTile tile, CaptureFrame frame, int guard) throws IOException {
        ByteBuffer row = buffer(Math.multiplyExact(tile.width(), 4));
        for (int y = 0; y < tile.height(); y++) {
            row.clear();
            for (int x = 0; x < tile.width(); x++) row.putInt(frame.argb(x + guard, y + guard));
            row.flip();
            long offset = ((long) (tile.y() + y) * size.width() + tile.x()) * 4;
            while (row.hasRemaining()) offset += channel.write(row, offset);
        }
    }

    public void putRow(int y, int[] pixels) throws IOException {
        ByteBuffer row = buffer(Math.multiplyExact(pixels.length, 4));
        for (int pixel : pixels) row.putInt(pixel);
        row.flip();
        long offset = (long) y * size.width() * 4;
        while (row.hasRemaining()) offset += channel.write(row, offset);
    }

    public void releaseBuffer() {
        buffer = null;
        CaptureMemoryReservation previous = bufferReservation;
        bufferReservation = null;
        if (previous != null) previous.close();
    }

    public void readSpan(int y, int x, int count, int[] target, int targetOffset, ByteBuffer shared)
        throws IOException {
        shared.clear()
            .limit(Math.multiplyExact(count, 4));
        long offset = ((long) y * size.width() + x) * 4;
        while (shared.hasRemaining()) {
            int read = channel.read(shared, offset);
            if (read < 0) throw new IOException("Incomplete scroll capture source");
            offset += read;
        }
        shared.flip();
        for (int index = 0; index < count; index++) target[targetOffset + index] = shared.getInt();
    }

    @Override
    public void row(int y, int[] target) throws IOException {
        ByteBuffer row = buffer(Math.multiplyExact(size.width(), 4));
        long offset = (long) y * size.width() * 4;
        while (row.hasRemaining()) {
            int count = channel.read(row, offset);
            if (count < 0) throw new IOException("Incomplete high-resolution capture source");
            offset += count;
        }
        row.flip();
        for (int x = 0; x < size.width(); x++) target[x] = CaptureRowComposer.composite(row.getInt(), background);
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        CaptureCleanup cleanup = new CaptureCleanup();
        cleanup.run(channel::close);
        cleanup.run(this::releaseBuffer);
        cleanup.run(() -> Files.deleteIfExists(path));
        if (cleanup.failure() instanceof IOException error) throw error;
        cleanup.rethrow();
    }
}
