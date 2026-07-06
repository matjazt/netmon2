package com.matjazt.netmon2.service;

import com.matjazt.tools.TimingStatistics;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Periodically logs accumulated REST API timing statistics and resets the counters.
 *
 * <p>The log interval is controlled by the {@code timing.log-cron} property in {@code
 * application.yaml}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TimingLoggingService {

    private final TimingStatistics timingStatistics;

    @PreDestroy
    @Scheduled(cron = "${timing.log-cron:0 0 * * * *}")
    public void logAndResetTimings() {
        String stats = timingStatistics.getAllStatisticsAsJson();
        if (stats != null) {
            log.info("timing statistics:\n{}", stats);
        }
        // Reset all timings by unregistering them. This also cancels any active timers, so they
        // won't be included in the next log interval if they happen to stop after the reset.
        timingStatistics.unregisterAll();
    }
}
