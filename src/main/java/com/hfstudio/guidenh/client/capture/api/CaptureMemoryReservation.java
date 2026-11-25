package com.hfstudio.guidenh.client.capture.api;

public class CaptureMemoryReservation implements AutoCloseable {

    private final CaptureMemoryAccount account;
    private final long bytes;
    private boolean closed;

    public CaptureMemoryReservation(CaptureMemoryAccount account, long bytes) {
        if (bytes < 0) throw new IllegalArgumentException("Capture reservations cannot be negative");
        this.account = account;
        this.bytes = bytes;
        if (account != null) account.reserve(bytes);
    }

    public long bytes() {
        return bytes;
    }

    public synchronized boolean matches(CaptureMemoryAccount account, long bytes) {
        return !closed && this.account == account && this.bytes == bytes;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (account != null) account.release(bytes);
    }
}
