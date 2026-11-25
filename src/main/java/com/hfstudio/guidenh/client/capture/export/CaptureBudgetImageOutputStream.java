package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import javax.imageio.stream.FileImageOutputStream;

public class CaptureBudgetImageOutputStream extends FileImageOutputStream {

    private final long budget;

    public CaptureBudgetImageOutputStream(Path target, long budget) throws IOException {
        super(target.toFile());
        this.budget = budget;
    }

    private void check(int count) throws IOException {
        if (count > budget - getStreamPosition())
            throw new IOException("GIF output exceeds the configured disk budget");
    }

    @Override
    public void write(int value) throws IOException {
        flushBits();
        check(1);
        super.write(value);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        flushBits();
        check(length);
        super.write(bytes, offset, length);
    }
}
