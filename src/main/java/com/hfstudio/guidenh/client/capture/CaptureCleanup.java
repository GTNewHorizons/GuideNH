package com.hfstudio.guidenh.client.capture;

public class CaptureCleanup {

    private Throwable failure;

    public CaptureCleanup() {}

    public CaptureCleanup(Throwable failure) {
        this.failure = failure;
    }

    public void run(Action action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
    }

    public void close(AutoCloseable resource) {
        if (resource != null) run(resource::close);
    }

    public Throwable failure() {
        return failure;
    }

    public void rethrow() {
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException error) throw error;
        if (failure != null) throw new IllegalStateException("Could not release all capture resources", failure);
    }

    @FunctionalInterface
    public interface Action {

        void run() throws Exception;
    }
}
