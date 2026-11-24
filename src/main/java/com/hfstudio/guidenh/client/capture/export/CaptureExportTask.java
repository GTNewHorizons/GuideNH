package com.hfstudio.guidenh.client.capture.export;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CaptureExportTask<T> implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger("GuideNH GUI export");
    private final Supplier<T> action;
    private final Runnable releaseQueued;
    private final Consumer<CaptureExportTask<T>> finished;
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private final AtomicBoolean claimed = new AtomicBoolean();
    private volatile Thread runner;

    public CaptureExportTask(Supplier<T> action, Runnable releaseQueued, Consumer<CaptureExportTask<T>> finished) {
        this.action = action;
        this.releaseQueued = releaseQueued;
        this.finished = finished;
        future.whenComplete((result, error) -> { if (future.isCancelled()) cancelWork(); });
    }

    public CompletableFuture<T> future() {
        return future;
    }

    public void cancel() {
        future.cancel(true);
    }

    private void cancelWork() {
        if (claimed.compareAndSet(false, true)) {
            try {
                releaseQueued.run();
            } catch (RuntimeException error) {
                LOGGER.error("Could not release a queued capture source", error);
            } finally {
                finished.accept(this);
            }
        } else {
            Thread thread = runner;
            if (thread != null) thread.interrupt();
        }
    }

    @Override
    public void run() {
        if (!claimed.compareAndSet(false, true)) return;
        runner = Thread.currentThread();
        try {
            if (future.isCancelled()) releaseQueued.run();
            else future.complete(action.get());
        } catch (CancellationException error) {
            future.cancel(false);
        } catch (Throwable error) {
            future.completeExceptionally(error);
        } finally {
            runner = null;
            finished.accept(this);
        }
    }
}
