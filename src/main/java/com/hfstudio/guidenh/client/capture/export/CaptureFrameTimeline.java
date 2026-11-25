package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CaptureFrameTimeline implements AutoCloseable {

    private final Path path;
    private final FileChannel channel;
    private final ByteBuffer value = ByteBuffer.allocate(Long.BYTES);
    private int count;
    private long last;
    private boolean closed;

    public CaptureFrameTimeline(Path directory) throws IOException {
        path = Files.createTempFile(directory, ".guidenh_times_", ".part");
        try {
            channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException | RuntimeException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    public int size() {
        return count;
    }

    public long byteSize() {
        return (long) count * Long.BYTES;
    }

    public long last() {
        if (count == 0) throw new IllegalStateException("The frame timeline is empty");
        return last;
    }

    public void append(long timestamp) throws IOException {
        checkOpen();
        if (timestamp < 0 || count > 0 && timestamp < last)
            throw new IllegalArgumentException("Frame times must be nonnegative and ordered");
        int nextCount = Math.incrementExact(count);
        value.clear();
        value.putLong(timestamp)
            .flip();
        long offset = byteSize();
        try {
            while (value.hasRemaining()) offset += channel.write(value, offset);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                channel.truncate(byteSize());
            } catch (IOException | RuntimeException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
        count = nextCount;
        last = timestamp;
    }

    public long time(int index) throws IOException {
        checkOpen();
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException(index);
        value.clear();
        long offset = (long) index * Long.BYTES;
        while (value.hasRemaining()) {
            int read = channel.read(value, offset);
            if (read < 0) throw new IOException("Incomplete animation frame timeline");
            offset += read;
        }
        value.flip();
        return value.getLong();
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException("The frame timeline is closed");
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            channel.close();
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
