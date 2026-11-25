package com.hfstudio.guidenh.guide.internal.resource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class GuideBoundedInputStream extends FilterInputStream {

    private long remaining;

    public GuideBoundedInputStream(InputStream input, long maxBytes) {
        super(input);
        if (maxBytes < 0) {
            throw new IllegalArgumentException("A nonnegative input limit is required");
        }
        remaining = maxBytes;
    }

    @Override
    public int read() throws IOException {
        int value = in.read();
        if (value >= 0) {
            if (remaining == 0) {
                throw new IOException("Guide input exceeds its byte limit");
            }
            remaining--;
        }
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        if (length == 0) return 0;
        if (remaining == 0) return read() < 0 ? -1 : 0;
        int count = in.read(bytes, offset, (int) Math.min(length, remaining));
        if (count > 0) remaining -= count;
        return count;
    }

    @Override
    public long skip(long count) throws IOException {
        long skipped = in.skip(Math.max(0L, Math.min(count, remaining)));
        remaining -= skipped;
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("Bounded guide input does not support resetting");
    }
}
