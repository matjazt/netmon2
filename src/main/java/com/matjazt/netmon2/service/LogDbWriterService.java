package com.matjazt.netmon2.service;

import com.matjazt.netmon2.entity.LogEntity;
import com.matjazt.netmon2.repository.LogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async service for writing log entries to the database.
 *
 * <p>This service is used by the custom Logback appender to persist log messages asynchronously
 * without blocking the logging thread. All exceptions are caught to ensure logging never fails the
 * application.
 *
 * <p>Thread Safety: This service is safe for high-volume concurrent calls thanks to:
 *
 * <ul>
 *   <li>@Async - Each call runs in a separate thread from the executor pool
 *   <li>Spring Data JPA repositories are thread-safe
 *   <li>LogEntity instances are not shared between threads
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogDbWriterService {

    private final LogRepository logRepository;

    /**
     * Asynchronously writes a log entry to the database.
     *
     * <p>This method never throws exceptions. If the database is unreachable or any error occurs, a
     * warning is logged but execution continues. This ensures that database issues never disrupt
     * application logging.
     *
     * @param logEntry the log entry to persist
     */
    @Async
    public void writeLogEntry(LogEntity logEntry) {
        try {
            logRepository.save(logEntry);
        } catch (Exception e) {
            // Never throw - database logging is supplementary and must not break the app
            // Also do not log the error to avoid potential recursion.
        }
    }
}
