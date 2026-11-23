package com.hfstudio.guidenh.client.capture.export;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CaptureBudgetOutputStream extends FilterOutputStream {

    private final long budget;
    private long written;

    public CaptureBudgetOutputStream(OutputStream output, long budget) {
        super(output);
        this.budget = budget;
    }

    private void check(int count) throws IOException {
        if (count > budget - written) throw new IOException("Capture output exceeds the remaining disk budget");
    }

    @Override
    public void write(int value) throws IOException {
        check(1);
        out.write(value);
        written++;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        check(length);
        out.write(bytes, offset, length);
        written += length;
    }
}
