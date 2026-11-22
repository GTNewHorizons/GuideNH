package com.hfstudio.guidenh.client.capture.render;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
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

    public CaptureTileStore(Path directory, CaptureOutputSize size, int background, long diskBudget)
        throws IOException {
        if (size.rgbaBytes() > diskBudget) {
            throw new IllegalArgumentException("The high-resolution source exceeds the temporary disk budget");
        }
        Files.createDirectories(directory);
        path = Files.createTempFile(directory, ".guidenh_tiles_", ".rgba");
        FileChannel opened = null;
        try {
            opened = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
            opened.write(ByteBuffer.wrap(new byte[1]), size.rgbaBytes() - 1);
        } catch (IOException error) {
            if (opened != null) opened.close();
            Files.deleteIfExists(path);
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
        if (buffer == null || buffer.capacity() < count) buffer = ByteBuffer.allocate(count)
            .order(ByteOrder.LITTLE_ENDIAN);
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
        try {
            channel.close();
        } finally {
            buffer = null;
            Files.deleteIfExists(path);
        }
    }
}
