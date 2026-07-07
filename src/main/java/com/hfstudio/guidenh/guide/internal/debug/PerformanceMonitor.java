package com.hfstudio.guidenh.guide.internal.debug;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.config.ModConfig;

/**
 * Tracks FPS and memory usage for debug overlay.
 * Optimized for zero overhead when debug mode is disabled.
 */
public class PerformanceMonitor {

    private static final int SAMPLE_SIZE = 20;
    private final long[] frameTimes = new long[SAMPLE_SIZE];
    private int frameIndex = 0;
    private long lastFrameTime = 0;
    private int calculatedFps = 0;
    private long lastFpsUpdate = 0;
    private static final long FPS_UPDATE_INTERVAL_MS = 500;

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private long usedMemoryMB = 0;
    private long maxMemoryMB = 0;
    private long lastMemoryUpdate = 0;
    private static final long MEMORY_UPDATE_INTERVAL_MS = 1000;

    public void onFrameStart() {
        if (!ModConfig.debug.guiDebugMode) {
            return;
        }

        long currentTime = System.nanoTime();
        if (lastFrameTime != 0) {
            frameTimes[frameIndex] = currentTime - lastFrameTime;
            frameIndex = (frameIndex + 1) % SAMPLE_SIZE;
        }
        lastFrameTime = currentTime;

        long now = System.currentTimeMillis();
        if (now - lastFpsUpdate >= FPS_UPDATE_INTERVAL_MS) {
            calculateFps();
            lastFpsUpdate = now;
        }

        if (now - lastMemoryUpdate >= MEMORY_UPDATE_INTERVAL_MS) {
            updateMemoryUsage();
            lastMemoryUpdate = now;
        }
    }

    private void calculateFps() {
        long sum = 0;
        int count = 0;
        for (long frameTime : frameTimes) {
            if (frameTime > 0) {
                sum += frameTime;
                count++;
            }
        }
        if (count > 0) {
            long avgFrameTime = sum / count;
            calculatedFps = avgFrameTime > 0 ? (int) (1_000_000_000L / avgFrameTime) : 0;
        }
    }

    private void updateMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        usedMemoryMB = heapUsage.getUsed() / (1024 * 1024);
        maxMemoryMB = heapUsage.getMax() / (1024 * 1024);
    }

    public int getFps() {
        if (!ModConfig.debug.guiDebugMode) {
            return Minecraft.getMinecraft().debug.split(" ")[0].equals("") ? 0
                : Integer.parseInt(Minecraft.getMinecraft().debug.split(" ")[0]);
        }
        return calculatedFps;
    }

    public long getUsedMemoryMB() {
        return usedMemoryMB;
    }

    public long getMaxMemoryMB() {
        return maxMemoryMB;
    }

    public int getMemoryPercentage() {
        return maxMemoryMB > 0 ? (int) ((usedMemoryMB * 100) / maxMemoryMB) : 0;
    }
}
