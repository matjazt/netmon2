package com.matjazt.tools;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TimingStatistics {

    private final ConcurrentHashMap<String, SingleTiming> timings = new ConcurrentHashMap<>();

    public record SingleTimingWithTimer(SingleTiming singleTiming, int timerHandle) {}
    ;

    /**
     * Registers a new event type if needed and returns its SingleTiming handle.
     *
     * @throws IllegalArgumentException if the event type is already registered.
     */
    public SingleTiming getTimingForEventType(String eventTypeName) {
        SingleTiming newTiming = new SingleTiming();
        SingleTiming existing = timings.putIfAbsent(eventTypeName, newTiming);
        if (existing != null) {
            log.trace("registerEventType: '{}' is already registered", eventTypeName);
            return existing;
        }
        log.trace("registerEventType: '{}' registered", eventTypeName);
        return newTiming;
    }

    public SingleTiming getTimingForEventType(Object callingObject, String eventTypeShortName) {
        return getTimingForEventType(
                callingObject.getClass().getSimpleName() + "." + eventTypeShortName);
    }

    public SingleTimingWithTimer startTimer(Object callingObject, String eventTypeShortName) {
        return startTimer(callingObject.getClass().getSimpleName() + "." + eventTypeShortName);
    }

    public SingleTimingWithTimer startTimer(String eventTypeFullName) {
        SingleTiming singleTiming = getTimingForEventType(eventTypeFullName);
        int timerHandle = singleTiming.startTimer();
        return new SingleTimingWithTimer(singleTiming, timerHandle);
    }

    /** Unregisters a timing by object reference. Silently ignores if not found. */
    public void unregisterTiming(SingleTiming singleTiming) {
        boolean removed = timings.entrySet().removeIf(e -> e.getValue() == singleTiming);
        if (removed) {
            log.trace("unregisterTiming(ref): removed");
        } else {
            log.warn("unregisterTiming(ref): not found");
        }
    }

    /** Unregisters a timing by event type name. Silently ignores if not found. */
    public void unregisterTiming(String eventTypeName) {
        SingleTiming removed = timings.remove(eventTypeName);
        if (removed != null) {
            log.trace("unregisterTiming: '{}' removed", eventTypeName);
        } else {
            log.warn("unregisterTiming: '{}' not found", eventTypeName);
        }
    }

    /** Removes all registered event types. */
    public void unregisterAll() {
        int count = timings.size();
        timings.clear();
        log.trace("unregisterAll: {} timing(s) removed", count);
    }

    /**
     * Returns statistics for all registered event types as a human readable JSON string, sorted
     * alphabetically by event type name with all columns aligned. WARNING: event names are not
     * escaped, so this is only safe if event type names are controlled and do not contain special
     * characters.
     */
    public String getAllStatisticsAsJson() {
        if (timings.isEmpty()) {
            return null;
        }

        // Snapshot and filter out empty timings, sort by event type name
        List<Map.Entry<String, SingleTiming>> allEntries = new ArrayList<>(timings.entrySet());
        var entries =
                allEntries.stream()
                        .filter(e -> e.getValue().getEventCount() > 0)
                        .sorted(Map.Entry.comparingByKey())
                        .toList();
        if (entries.isEmpty()) {
            return "";
        }

        List<SingleTiming.TimingStats> stats =
                entries.stream().map(e -> e.getValue().getStatistics()).toList();

        // Compute column widths from entries that have data
        int nameWidth = entries.stream().mapToInt(e -> e.getKey().length()).max().orElse(0);
        int countWidth =
                stats.stream().mapToInt(s -> Long.toString(s.count()).length()).max().orElse(1);
        int minWidth =
                stats.stream().mapToInt(s -> Long.toString(s.minMs()).length()).max().orElse(1);

        int maxWidth =
                stats.stream().mapToInt(s -> Long.toString(s.maxMs()).length()).max().orElse(1);

        int avgWidth =
                stats.stream().mapToInt(s -> Long.toString(s.avgMs()).length()).max().orElse(1);

        nameWidth += 2; // quotes around event type name

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < entries.size(); i++) {

            if (i > 0) {
                sb.append(",");
            }

            String name = "\"" + entries.get(i).getKey() + "\"";
            SingleTiming.TimingStats s = stats.get(i);
            sb.append(String.format("\n{%-" + nameWidth + "s: ", name));

            sb.append(
                    String.format(
                            "{ \"count\": %"
                                    + countWidth
                                    + "d, \"min\": %"
                                    + minWidth
                                    + "d, \"max\": %"
                                    + maxWidth
                                    + "d, \"avg\": %"
                                    + avgWidth
                                    + "d }}",
                            s.count(),
                            s.minMs(),
                            s.maxMs(),
                            s.avgMs()));
        }
        sb.append("\n]");
        return sb.toString();
    }

    /** Resets statistics on all registered SingleTiming objects, keeping them registered. */
    public void resetAll() {
        int count = timings.size();
        timings.values().forEach(SingleTiming::reset);
        log.trace("resetAll: {} timing(s) reset", count);
    }
}
