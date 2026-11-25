package com.hfstudio.guidenh.client.capture.recording;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;
import com.hfstudio.guidenh.client.capture.export.CaptureSpritePageLayout;

public class CaptureRecording implements AutoCloseable {

    private final CaptureExportRequest request;
    private final CaptureRecordingOptions options;
    private final ArrayBlockingQueue<CaptureRecordedFrame> queue;
    private final AtomicLong queuedBytes = new AtomicLong();
    private final long queueBudget;
    private final long sourceFrameBytes;
    private final long sampleIntervalNanos;
    private final long started;
    private long nextSample;
    private long pausedAt;
    private long pausedNanos;
    private int acceptedFrames;
    private int skippedFrames;
    private boolean queueLimitReached;
    private volatile boolean finished;
    private volatile boolean cancelled;
    private volatile long endTime;
    private CompletableFuture<CaptureOutputArtifact> completion;
    private CaptureMemoryAccount memory;

    public CaptureRecording(CaptureExportRequest request, CaptureRecordingOptions options, long now) {
        this.request = request;
        this.options = options;
        started = now;
        nextSample = now;
        sampleIntervalNanos = 1_000_000_000L / options.fps();
        CaptureComposition composition = CaptureRowComposer.compose(request);
        CaptureOutputSize output = request.options()
            .resolution()
            .resolve(
                composition.size()
                    .width(),
                composition.size()
                    .height());
        queueBudget = options.memoryBudgetBytes() - Math.multiplyExact(output.rgbaBytes(), 3);
        sourceFrameBytes = new CaptureOutputSize(
            request.viewport()
                .pixelWidth(),
            request.viewport()
                .pixelHeight()).rgbaBytes();
        if (queueBudget < sourceFrameBytes) throw new IllegalArgumentException(
            "Recording exceeds the memory budget; reduce output dimensions or increase the budget");
        if (options.format()
            .equals("gif") && (output.width() > 65535 || output.height() > 65535))
            throw new IllegalArgumentException("GIF dimensions exceed 65535 pixels");
        if (options.format()
            .equals("sprites")) CaptureSpritePageLayout.create(output, options);
        queue = new ArrayBlockingQueue<>(options.queueFrames());
    }

    public CaptureExportRequest request() {
        return request;
    }

    public synchronized void memoryAccount(CaptureMemoryAccount account) {
        if (account == null) return;
        if (memory != null && memory != account)
            throw new IllegalStateException("A recording cannot change its allocation budget");
        for (CaptureRecordedFrame frame : queue) frame.frame()
            .account(account);
        memory = account;
    }

    public CaptureRecordingOptions options() {
        return options;
    }

    public void completion(CompletableFuture<CaptureOutputArtifact> future) {
        completion = future;
    }

    public CompletableFuture<CaptureOutputArtifact> completion() {
        return completion;
    }

    public boolean paused() {
        return pausedAt != 0;
    }

    public boolean finished() {
        return finished;
    }

    public boolean sampling() {
        return !finished && !paused();
    }

    public boolean cancelled() {
        return cancelled;
    }

    public int frameCount() {
        return acceptedFrames;
    }

    public int skippedFrames() {
        return skippedFrames;
    }

    public boolean queueLimitReached() {
        return queueLimitReached;
    }

    public long elapsed(long now) {
        return (pausedAt == 0 ? now : pausedAt) - started - pausedNanos;
    }

    public boolean needsFrame(long now) {
        return sampling() && now >= nextSample
            && (memory == null || memory.available() / 2 >= sourceFrameBytes)
            && queuedBytes.get() <= queueBudget - sourceFrameBytes
            && queue.remainingCapacity() > 0;
    }

    public void offer(CaptureFrame frame, long now) {
        offer(frame, now, List.of());
    }

    public synchronized void offer(CaptureFrame frame, long now, List<CaptureRegion> regions) {
        if (finished || paused() || now < nextSample) return;
        long missed = (now - nextSample) / sampleIntervalNanos;
        skippedFrames = (int) Math.min(Integer.MAX_VALUE, (long) skippedFrames + missed);
        nextSample += (missed + 1) * sampleIntervalNanos;
        long bytes = frame.byteSize();
        if (bytes > queueBudget - queuedBytes.get()) {
            rejected(now);
            return;
        }
        List<CaptureRegion> snapshot = List.copyOf(regions);
        CaptureRecordedFrame owned = new CaptureRecordedFrame(
            frame.account(memory)
                .retain(),
            elapsed(now),
            snapshot);
        if (finished) {
            owned.close();
            return;
        }
        queuedBytes.addAndGet(bytes);
        if (!queue.offer(owned)) {
            owned.close();
            queuedBytes.addAndGet(-bytes);
            rejected(now);
            return;
        }
        acceptedFrames++;
        if (options.frameLimit() > 0 && acceptedFrames >= options.frameLimit()) stop(now);
    }

    private void rejected(long now) {
        skippedFrames++;
        if (options.strictQueue()) {
            queueLimitReached = true;
            stop(now);
        }
    }

    public void memoryUnavailable(long now) {
        if (finished || paused()) return;
        nextSample = now + sampleIntervalNanos;
        rejected(now);
    }

    public CaptureRecordedFrame poll() throws InterruptedException {
        return queue.poll(100, TimeUnit.MILLISECONDS);
    }

    public void consumed(CaptureRecordedFrame frame) {
        queuedBytes.addAndGet(
            -frame.frame()
                .byteSize());
        frame.close();
    }

    public boolean drained() {
        return finished && queue.isEmpty();
    }

    public long endTime() {
        return endTime;
    }

    public void tick(long now) {
        if (!finished && options.durationSeconds() > 0 && elapsed(now) >= options.durationSeconds() * 1_000_000_000L) {
            stop(now);
        }
        if (!finished && !paused()
            && now >= nextSample
            && (queue.remainingCapacity() == 0 || queuedBytes.get() > queueBudget - sourceFrameBytes
                || memory != null && memory.available() / 2 < sourceFrameBytes)) {
            nextSample = now + sampleIntervalNanos;
            rejected(now);
        }
    }

    public void togglePause(long now) {
        if (finished) return;
        if (pausedAt == 0) pausedAt = now;
        else {
            pausedNanos += now - pausedAt;
            pausedAt = 0;
            nextSample = now;
        }
    }

    public void stop(long now) {
        endTime = elapsed(now);
        finished = true;
    }

    @Override
    public synchronized void close() {
        cancelled = true;
        finished = true;
        CaptureCleanup cleanup = new CaptureCleanup();
        CaptureRecordedFrame frame;
        while ((frame = queue.poll()) != null) {
            CaptureRecordedFrame previous = frame;
            cleanup.run(() -> consumed(previous));
        }
        cleanup.rethrow();
    }
}
