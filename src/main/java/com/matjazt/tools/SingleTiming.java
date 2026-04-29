package com.matjazt.tools;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SingleTiming {

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private static class SingleTimer {
        final long startNanos = System.nanoTime();
        boolean cancelled = false;
    }

    /** Immutable snapshot of accumulated statistics. */
    public record TimingStats(long count, long minMs, long maxMs, long avgMs) {}

    // -------------------------------------------------------------------------
    // State (all guarded by lock)
    // -------------------------------------------------------------------------

    private final Object lock = new Object();

    /** Active timers, keyed by handle. Cancelled timers stay here until stopTimer removes them. */
    private final Map<Integer, SingleTimer> activeTimers = new HashMap<>();

    private int nextHandle = 0;

    private long count = 0;
    private long totalMs = 0;
    private long minMs = Long.MAX_VALUE;
    private long maxMs = Long.MIN_VALUE;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Starts a timer and returns its handle. Handles are unique for the lifetime of this object.
     */
    public int startTimer() {
        synchronized (lock) {
            int handle = ++nextHandle;
            activeTimers.put(handle, new SingleTimer());
            log.trace("startTimer: handle={}, activeTimers={}", handle, activeTimers.size());
            return handle;
        }
    }

    /**
     * Stops the timer for the given handle, records the elapsed time, and returns it in ms. Returns
     * -1 if the handle is unknown or the timer was cancelled by reset().
     */
    public long stopTimer(int timerHandle) {
        synchronized (lock) {
            var timer = activeTimers.remove(timerHandle);
            if (timer == null) {
                log.warn(
                        "stopTimer: handle={} not found (already stopped, or never started)",
                        timerHandle);
                return -1L;
            }
            if (timer.cancelled) {
                log.trace("stopTimer: handle={} was cancelled by reset(), ignoring", timerHandle);
                return -1L;
            }
            long elapsedMs = (System.nanoTime() - timer.startNanos) / 1_000_000L;
            count++;
            totalMs += elapsedMs;
            if (elapsedMs < minMs) minMs = elapsedMs;
            if (elapsedMs > maxMs) maxMs = elapsedMs;
            log.trace("stopTimer: handle={}, elapsed={}ms", timerHandle, elapsedMs);
            return elapsedMs;
        }
    }

    /**
     * Resets all accumulated statistics and flags all currently active timers as cancelled.
     * Cancelled timers remain in the map; they will be cleaned up when stopTimer() is eventually
     * called.
     */
    public void reset() {
        synchronized (lock) {
            int activeCount = activeTimers.size();
            activeTimers.values().forEach(t -> t.cancelled = true);
            count = 0;
            totalMs = 0;
            minMs = Long.MAX_VALUE;
            maxMs = Long.MIN_VALUE;
            log.trace(
                    "reset: statistics cleared, {} active timer(s) flagged as cancelled",
                    activeCount);
        }
    }

    /** Returns a snapshot of accumulated statistics, suitable for programmatic use. */
    public TimingStats getStatistics() {
        synchronized (lock) {
            if (count == 0) {
                return new TimingStats(0, 0, 0, 0);
            }
            return new TimingStats(count, minMs, maxMs, totalMs / count);
        }
    }

    /** Returns the number of completed (non-cancelled) timer events recorded so far. */
    public long getEventCount() {
        synchronized (lock) {
            return count;
        }
    }
}
