package com.umeed.common;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts requests per key (an account or an address) in one-minute windows, in memory. Enough for one server;
 * the counts start again when the backend restarts. Thread-safe.
 */
final class RateLimiter {

    /** Old windows are swept out when this many keys are being tracked. */
    private static final int SWEEP_ABOVE = 10_000;

    private record Window(long minute, int count) {
    }

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Counts one request for the key and says whether it is still within the limit for this minute. */
    boolean tryAcquire(String key, int limitPerMinute) {
        long minute = clock.millis() / 60_000;
        Window window = windows.compute(key, (k, current) ->
                current == null || current.minute() != minute
                        ? new Window(minute, 1)
                        : new Window(minute, current.count() + 1));
        if (windows.size() > SWEEP_ABOVE) {
            windows.values().removeIf(old -> old.minute() < minute);
        }
        return window.count() <= limitPerMinute;
    }

    int trackedKeys() {
        return windows.size();
    }
}
