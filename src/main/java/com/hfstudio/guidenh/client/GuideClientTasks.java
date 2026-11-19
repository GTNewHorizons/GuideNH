package com.hfstudio.guidenh.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;

public class GuideClientTasks {

    protected GuideClientTasks() {}

    public static <T> T call(Supplier<T> task) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) throw new IllegalStateException("Minecraft client is not ready");
        if (minecraft.func_152345_ab()) return task.get();
        CompletableFuture<T> future = new CompletableFuture<>();
        GuideNhClientTaskScheduler.execute(() -> {
            if (future.isCancelled()) return;
            try {
                future.complete(task.get());
            } catch (Throwable exception) {
                future.completeExceptionally(exception);
            }
        });
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            future.cancel(false);
            Thread.currentThread()
                .interrupt();
            throw new IllegalStateException("Interrupted while waiting for the Minecraft client", exception);
        } catch (TimeoutException exception) {
            future.cancel(false);
            throw new IllegalStateException("Timed out waiting for the Minecraft client", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Minecraft client operation failed", cause);
        }
    }
}
