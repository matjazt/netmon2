package com.matjazt.netmon2.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.LogEntity;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.service.LogDbWriterService;

import java.time.LocalDateTime;

/**
 * Custom Logback appender that captures log messages referencing network/device entities.
 *
 * <p>This appender inspects log event arguments for {@link NetworkEntity} or {@link DeviceEntity}
 * instances and writes them to the database asynchronously. It operates alongside regular file
 * logging without interfering.
 *
 * <p>Key Design Principles:
 *
 * <ul>
 *   <li>Extremely lightweight - only entity detection and async delegation
 *   <li>Never throws exceptions - all errors are silently caught
 *   <li>Never blocks - database writes happen asynchronously
 *   <li>Fail-safe - if database writer unavailable, silently skip
 * </ul>
 *
 * <p>Usage in code:
 *
 * <pre>{@code
 * log.info("Device went offline: {}", deviceEntity, deviceEntity);
 * log.warn("Network timeout: {}", networkEntity, networkEntity);
 * }</pre>
 *
 * <p>Note: Arguments are passed twice (once for formatting, once for entity detection) because the
 * formatted message doesn't preserve object references.
 */
public class NetworkLogAppender extends AppenderBase<ILoggingEvent> {

    private volatile LogDbWriterService logDbWriter;
    private volatile boolean writerResolved = false;

    // Do NOT attempt to get Spring beans in start() - Spring is not ready yet

    private LogDbWriterService getWriter() {
        if (!writerResolved) {
            synchronized (this) {
                if (!writerResolved) {
                    logDbWriter = SpringContextHelper.getBean(LogDbWriterService.class);
                    if (logDbWriter != null) {
                        writerResolved = true;
                    }
                }
            }
        }
        return logDbWriter;
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            LogDbWriterService writer = getWriter();
            if (writer == null) {
                return;
            }

            // Extract entities from log arguments
            NetworkEntity networkEntity = null;
            DeviceEntity deviceEntity = null;

            Object[] args = event.getArgumentArray();
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof NetworkEntity) {
                        networkEntity = (NetworkEntity) arg;
                    } else if (arg instanceof DeviceEntity) {
                        deviceEntity = (DeviceEntity) arg;
                    }
                }
            }

            // If we have a device but not a network, get network from device
            if (networkEntity == null && deviceEntity != null) {
                networkEntity = deviceEntity.getNetwork();
            }

            // Require at least a NetworkEntity for database logging
            if (networkEntity == null) {
                return;
            }

            // Build the log entity
            LogEntity logEntry =
                    new LogEntity(
                            null,
                            LocalDateTime.now(),
                            event.getLevel().toInt(),
                            event.getLoggerName(),
                            truncateMessage(event.getFormattedMessage()),
                            networkEntity,
                            deviceEntity);

            // Async write - never blocks
            logDbWriter.writeLogEntry(logEntry);

        } catch (Exception e) {
            // Never throw - just log to Logback's internal status manager
            addWarn("Error processing log event: " + e.getMessage());
        }
    }

    /**
     * Truncates message to fit database column length (500 chars).
     *
     * @param message the log message
     * @return truncated message if needed
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() > 500) {
            return message.substring(0, 497) + "...";
        }
        return message;
    }
}
