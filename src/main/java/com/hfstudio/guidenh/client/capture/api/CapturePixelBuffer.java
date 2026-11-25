package com.hfstudio.guidenh.client.capture.api;

import java.util.Objects;

public class CapturePixelBuffer implements AutoCloseable {

    private int[] pixels;
    private final long bytes;
    private int references = 1;
    private CaptureMemoryAccount account;
    private CaptureMemoryReservation reservation;

    public CapturePixelBuffer(int[] pixels) {
        this.pixels = Objects.requireNonNull(pixels);
        bytes = (long) pixels.length * Integer.BYTES;
    }

    public static CapturePixelBuffer allocate(int count, CaptureMemoryAccount account) {
        if (count < 1) throw new IllegalArgumentException("Capture pixel buffers require a positive size");
        CaptureMemoryReservation reservation = new CaptureMemoryReservation(account, (long) count * Integer.BYTES);
        return allocate(count, account, reservation);
    }

    public static CapturePixelBuffer allocate(int count, CaptureMemoryAccount account,
        CaptureMemoryReservation reservation) {
        Objects.requireNonNull(reservation);
        try {
            if (count < 1 || !reservation.matches(account, (long) count * Integer.BYTES))
                throw new IllegalArgumentException("The pixel reservation must match the buffer size");
            CapturePixelBuffer buffer = new CapturePixelBuffer(new int[count]);
            buffer.account = account;
            buffer.reservation = reservation;
            return buffer;
        } catch (RuntimeException | Error failure) {
            try {
                reservation.close();
            } catch (RuntimeException cleanup) {
                if (failure != cleanup) failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    public synchronized int[] array() {
        if (references == 0) throw new IllegalStateException("The capture pixel buffer is released");
        return pixels;
    }

    public synchronized CapturePixelBuffer retain() {
        if (references == 0) throw new IllegalStateException("The capture pixel buffer is released");
        references = Math.incrementExact(references);
        return this;
    }

    public synchronized CaptureMemoryAccount memoryAccount() {
        return account;
    }

    public synchronized void account(CaptureMemoryAccount next) {
        if (references == 0) throw new IllegalStateException("The capture pixel buffer is released");
        if (next == null || next == account) return;
        if (account != null) throw new IllegalStateException("The capture pixels already belong to another budget");
        reservation = new CaptureMemoryReservation(next, bytes);
        account = next;
    }

    @Override
    public synchronized void close() {
        if (references == 0) throw new IllegalStateException("The capture pixel buffer was released twice");
        if (--references != 0) return;
        pixels = null;
        if (reservation != null) reservation.close();
        reservation = null;
    }
}
