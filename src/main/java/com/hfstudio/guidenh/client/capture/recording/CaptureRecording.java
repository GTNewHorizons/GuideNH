package com.hfstudio.guidenh.client.capture.recording;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;

public class CaptureRecording implements AutoCloseable {

    private final CaptureExportRequest request;
    private final CaptureRecordingOptions options;
    private final ArrayBlockingQueue<CaptureRecordedFrame> queue;
    private final AtomicLong queuedBytes = new AtomicLong();
    private final long queueBudget;
    private final long started;
    private long nextSample;
    private long pausedAt;
    private long pausedNanos;
    private int acceptedFrames;
    private int skippedFrames;
    private volatile boolean finished;
    private volatile boolean cancelled;
    private volatile long endTime;
    private CompletableFuture<CaptureOutputArtifact> completion;

    public CaptureRecording(CaptureExportRequest request, CaptureRecordingOptions options, long now) {
        this.request = request;
        this.options = options;
        started = now;
        nextSample = now;
        CaptureComposition composition = CaptureRowComposer.compose(request);
        long outputBytes = request.options()
            .resolution()
            .resolve(
                composition.size()
                    .width(),
                composition.size()
                    .height())
            .rgbaBytes();
        queueBudget = options.memoryBudgetBytes() - outputBytes * 3;
        long sourceBytes = (long) request.viewport()
            .pixelWidth() * request.viewport()
                .pixelHeight()
            * 4;
        if (queueBudget < sourceBytes) throw new IllegalArgumentException(
            "Recording exceeds the memory budget; reduce output dimensions or increase the budget");
        if (options.format()
            .equals("gif")
            && (composition.size()
                .width() > 65535
                || composition.size()
                    .height() > 65535))
            throw new IllegalArgumentException("GIF dimensions exceed 65535 pixels");
        queue = new ArrayBlockingQueue<>(options.queueFrames());
    }

    public CaptureExportRequest request() {
        return request;
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

    public long elapsed(long now) {
        return (pausedAt == 0 ? now : pausedAt) - started - pausedNanos;
    }

    public boolean needsFrame(long now) {
        return sampling() && now >= nextSample
            && queuedBytes.get() + (long) request.viewport()
                .pixelWidth() * request.viewport()
                    .pixelHeight()
                * 4 <= queueBudget
            && queue.remainingCapacity() > 0;
    }

    public void offer(CaptureFrame frame, long now) {
        offer(frame, now, List.of());
    }

    public void offer(CaptureFrame frame, long now, List<CaptureRegion> regions) {
        if (finished || paused() || now < nextSample) return;
        long interval = 1_000_000_000L / options.fps();
        long missed = (now - nextSample) / interval;
        skippedFrames = (int) Math.min(Integer.MAX_VALUE, (long) skippedFrames + missed);
        nextSample += (missed + 1) * interval;
        long bytes = frame.byteSize();
        if (queuedBytes.addAndGet(bytes) > queueBudget) {
            queuedBytes.addAndGet(-bytes);
            rejected(now);
            return;
        }
        CaptureRecordedFrame owned = new CaptureRecordedFrame(frame.retain(), elapsed(now), regions);
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
            stop(now);
        }
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
            && (queue.remainingCapacity() == 0 || queuedBytes.get() + (long) request.viewport()
                .pixelWidth() * request.viewport()
                    .pixelHeight()
                * 4 > queueBudget)) {
            nextSample = now + 1_000_000_000L / options.fps();
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
    public void close() {
        cancelled = true;
        finished = true;
        CaptureRecordedFrame frame;
        while ((frame = queue.poll()) != null) consumed(frame);
    }
}
