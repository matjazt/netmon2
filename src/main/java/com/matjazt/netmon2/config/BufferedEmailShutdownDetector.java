package com.matjazt.netmon2.config;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BufferedEmailShutdownDetector {

    // we need mailSender dependency here to ensure @PreDestroy runs soon enough (hopefully)
    // Since BufferedEmailAppender is not a Spring bean, we cannot rely on its lifecycle for
    // shutdown detection, so we use this separate component to trigger email flushing on shutdown.
    private final JavaMailSender mailSender;

    @Setter private static volatile BufferedEmailAppender emailAppender;

    @PreDestroy
    void onShutdown() {
        // This method will be called when the application is shutting down
        // We can use this to trigger any final email sending if needed
        if (mailSender != null && emailAppender != null) {
            emailAppender.flushBeforeShutdown();
        }
    }
}
