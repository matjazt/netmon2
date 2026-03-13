package com.matjazt.netmon2.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

import com.matjazt.tools.SimpleTools;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Custom Logback appender that buffers log messages and sends them via email when either:
 *
 * <ul>
 *   <li>Buffer reaches maximum count ({@code maxCount})
 *   <li>Maximum delay time elapses ({@code maxDelaySeconds})
 * </ul>
 *
 * <p>Key Design Principles:
 *
 * <ul>
 *   <li>Lazy initialization — configuration is resolved from Spring context on first use
 *   <li>Thread-safe buffering with configurable thresholds
 *   <li>Time-based and count-based flushing
 *   <li>Graceful shutdown with buffer flush
 *   <li>Configuration via Spring properties using relaxed binding
 *   <li>Fail-safe — if Spring context is not ready, events are silently skipped
 *   <li>Self-healing — once Spring context is available, configuration is cached
 * </ul>
 */
public class BufferedEmailAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final TargetLengthBasedClassNameAbbreviator ABBREVIATOR =
            new TargetLengthBasedClassNameAbbreviator(50);

    // Lazy-resolved Spring configuration
    private volatile EmailLoggingProperties config;
    private volatile boolean configResolved = false;
    private volatile Level minLevel;

    // Lazy-resolved Spring mail sender
    private volatile JavaMailSender mailSender;
    private volatile boolean mailSenderResolved = false;

    // Internal state
    private final List<String> buffer = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private volatile LocalDateTime firstLogTime;
    private ScheduledExecutorService scheduler;
    private volatile boolean schedulerStarted;
    private volatile boolean shuttingDown;
    private volatile boolean enabled;

    // Do NOT attempt to get Spring beans in start() - Spring is not ready yet

    /**
     * Lazily resolve configuration from Spring context.
     *
     * @return configuration properties, or null if Spring context is not yet available
     */
    private EmailLoggingProperties getConfig() {
        if (!configResolved) {
            synchronized (this) {
                if (!configResolved) {
                    config = SpringContextHelper.getBean(EmailLoggingProperties.class);
                    if (config != null) {
                        configResolved = true;
                        minLevel = Level.toLevel(config.getMinLevel(), Level.ERROR);

                        enabled =
                                config.getEmailFrom() != null
                                        && config.getEmailFrom().contains("@")
                                        && config.getEmailTo() != null
                                        && config.getEmailTo().contains("@");

                        addInfo(
                                "BufferedEmailAppender configuration resolved: enabled="
                                        + enabled
                                        + ", maxCount="
                                        + config.getMaxCount()
                                        + ", maxDelaySeconds="
                                        + config.getMaxDelaySeconds()
                                        + ", minLevel="
                                        + minLevel);
                    }
                }
            }
        }
        return config;
    }

    /**
     * Lazily resolve JavaMailSender from Spring context.
     *
     * @return JavaMailSender instance, or null if Spring context is not yet available
     */
    private JavaMailSender getMailSender() {
        if (!mailSenderResolved) {
            synchronized (this) {
                if (!mailSenderResolved) {
                    mailSender = SpringContextHelper.getBean(JavaMailSender.class);
                    if (mailSender != null) {
                        mailSenderResolved = true;
                        addInfo("BufferedEmailAppender mail sender resolved");
                    }
                }
            }
        }
        return mailSender;
    }

    /** Start scheduler for time-based flushing (deferred until first log event). */
    private void ensureSchedulerStarted() {
        if (!schedulerStarted) {
            synchronized (this) {
                if (!schedulerStarted) {
                    scheduler =
                            Executors.newSingleThreadScheduledExecutor(
                                    r -> {
                                        Thread t = new Thread(r, "BufferedEmailAppender-Scheduler");
                                        t.setDaemon(true);
                                        return t;
                                    });

                    // Check every 10 seconds if buffer needs to be flushed due to time
                    scheduler.scheduleAtFixedRate(
                            this::checkAndFlushByTime, 10, 10, TimeUnit.SECONDS);

                    schedulerStarted = true;
                    addInfo("BufferedEmailAppender scheduler started");
                }
            }
        }
    }

    @Override
    public void start() {
        // No validation here - configuration is resolved lazily on first use
        super.start();
        BufferedEmailShutdownDetector.setEmailAppender(this);
        addInfo(
                "BufferedEmailAppender started (configuration will be resolved from Spring"
                        + " context)");
    }

    @Override
    public void stop() {
        shuttingDown = true;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Flush remaining logs before shutdown; note that this will probably not work beacause
        // email sender may not be available during shutdown, but we can at least attempt it.
        flushBuffer();

        BufferedEmailShutdownDetector.setEmailAppender(null);
        super.stop();
        addInfo("BufferedEmailAppender stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            var cfg = getConfig();

            if (!enabled || cfg == null) {
                return;
            }

            // Filter by minimum level
            if (event.getLevel().toInt() < minLevel.toInt()) {
                return;
            }

            // Ensure scheduler is started (deferred until first log event)
            ensureSchedulerStarted();

            String formattedLog = formatLogEntry(event);

            lock.lock();
            try {
                // Initialize first log time if buffer was empty
                if (buffer.isEmpty()) {
                    firstLogTime = LocalDateTime.now();
                }

                buffer.add(formattedLog);

                // Flush if max count reached
                if (buffer.size() >= cfg.getMaxCount()) {
                    flushBufferInternal();
                }
            } finally {
                lock.unlock();
            }

        } catch (Exception e) {
            addError("Error appending log event: " + e.getMessage(), e);
        }
    }

    /** Format a single log entry as plain text line. */
    private String formatLogEntry(ILoggingEvent event) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String level = String.format("%-5s", event.getLevel().toString());
        String logger = ABBREVIATOR.abbreviate(event.getLoggerName());
        String message = SimpleTools.safeTruncate(event.getFormattedMessage(), 4000);
        String thread = event.getThreadName();

        // Extract exception if present
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            message += "\n" + ThrowableProxyUtil.asString(tp);
        }

        return String.format("%s %s [%s] %s - %s", timestamp, level, thread, logger, message);
    }

    /** Check if buffer should be flushed due to time elapsed (scheduled check). */
    private void checkAndFlushByTime() {
        EmailLoggingProperties cfg = getConfig();
        if (cfg == null) {
            return;
        }

        lock.lock();
        try {
            if (!buffer.isEmpty() && firstLogTime != null) {
                long secondsElapsed =
                        java.time.Duration.between(firstLogTime, LocalDateTime.now()).getSeconds();
                if (secondsElapsed >= cfg.getMaxDelaySeconds()) {
                    flushBufferInternal();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void flushBeforeShutdown() {
        shuttingDown = true;
        flushBuffer();
    }

    /** Public method to manually flush the buffer. */
    public void flushBuffer() {
        lock.lock();
        try {
            flushBufferInternal();
        } finally {
            lock.unlock();
        }
    }

    /** Internal flush method - must be called with lock held. */
    private void flushBufferInternal() {
        if (buffer.isEmpty()) {
            return;
        }

        try {
            List<String> logsToSend = new ArrayList<>(buffer);
            buffer.clear();
            firstLogTime = null;

            if (shuttingDown) {
                // If we're shutting down, send email synchronously to ensure it gets sent before
                // exit
                sendEmail(logsToSend);
            } else {
                // Send email asynchronously to not block logging thread
                sendEmailAsync(logsToSend);
            }
        } catch (Exception e) {
            addError("Error flushing buffer: " + e.getMessage(), e);
        }
    }

    /** Send email with buffered logs asynchronously. */
    private void sendEmailAsync(List<String> logs) {
        Thread emailThread =
                new Thread(
                        () -> {
                            try {
                                sendEmail(logs);
                            } catch (Exception e) {
                                addError("Error sending email: " + e.getMessage(), e);
                            }
                        },
                        "BufferedEmailAppender-EmailSender");
        emailThread.setDaemon(true);
        emailThread.start();
    }

    /** Send email with the provided log entries. */
    private void sendEmail(List<String> logs) {
        EmailLoggingProperties cfg = getConfig();
        if (cfg == null) {
            addError("Cannot send email - configuration not available");
            return;
        }

        JavaMailSender sender = getMailSender();
        if (sender == null) {
            addError("Cannot send email - mail sender not available");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(cfg.getEmailFrom());
        message.setTo(cfg.getEmailToArray());

        String subject =
                cfg.getEmailSubjectPrefix()
                        + " "
                        + logs.size()
                        + " log entries - "
                        + LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        message.setSubject(subject);

        String plainTextContent = buildPlainTextEmail(logs);
        message.setText(plainTextContent);

        sender.send(message);

        addInfo(
                "Email sent successfully with "
                        + logs.size()
                        + " log entries to "
                        + cfg.getEmailTo());
    }

    /** Build plain text email body with log entries. */
    private String buildPlainTextEmail(List<String> logs) {
        StringBuilder text = new StringBuilder();
        for (String log : logs) {
            text.append(log).append("\n");
        }

        return text.toString();
    }
}
