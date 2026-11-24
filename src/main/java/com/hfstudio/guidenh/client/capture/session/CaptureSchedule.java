package com.hfstudio.guidenh.client.capture.session;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class CaptureSchedule {

    private final long interval;
    private long nextTime;
    private int remaining;
    private boolean cancelled;

    public CaptureSchedule(int delaySeconds, String localTime, int intervalSeconds, int repeatCount, long now) {
        interval = intervalSeconds * 1_000_000_000L;
        remaining = repeatCount;
        long delay = delaySeconds * 1_000_000_000L;
        if (!localTime.isBlank()) {
            LocalDateTime dateTime;
            if (localTime.contains("-")) dateTime = LocalDateTime.parse(
                localTime.trim()
                    .replace(' ', 'T'));
            else {
                LocalDateTime localNow = LocalDateTime.now();
                dateTime = localNow.toLocalDate()
                    .atTime(LocalTime.parse(localTime.trim()));
                if (dateTime.isBefore(localNow)) dateTime = dateTime.plusDays(1);
            }
            delay = Math.max(
                0,
                Duration.between(
                    Instant.now(),
                    dateTime.atZone(ZoneId.systemDefault())
                        .toInstant())
                    .toNanos());
        }
        nextTime = Math.addExact(now, delay);
    }

    public boolean due(long now) {
        return !cancelled && remaining > 0 && now >= nextTime;
    }

    public int remaining() {
        return remaining;
    }

    public long secondsUntil(long now) {
        return Math.max(0, (nextTime - now + 999_999_999L) / 1_000_000_000L);
    }

    public void consumed(long now) {
        remaining--;
        nextTime = Math.addExact(now, interval);
    }

    public boolean complete() {
        return cancelled || remaining == 0;
    }

    public void cancel() {
        cancelled = true;
    }
}
