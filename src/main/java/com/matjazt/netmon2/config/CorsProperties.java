package com.matjazt.netmon2.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS configuration properties bound to the {@code netmon.cors} prefix.
 *
 * <p>Override via environment variables using Spring's relaxed binding, e.g.:
 *
 * <pre>
 *   NETMON_CORS_ALLOWED_ORIGINS=https://app.example.com
 *   NETMON_CORS_ALLOWED_ORIGINS=https://foo.com,https://bar.com   (comma list)
 * </pre>
 *
 * <p>For development, {@code allowed-origins: ["*"]} is the default. For production, replace with
 * the actual frontend origin(s).
 */
@Component
@ConfigurationProperties(prefix = "netmon.cors")
@Getter
@Setter
public class CorsProperties {

    /**
     * Allowed origins. Use {@code *} for any origin (development only).
     *
     * <p>Supports wildcard patterns, e.g. {@code https://*.example.com}.
     */
    private List<String> allowedOrigins = List.of("*");

    /** HTTP methods to allow. */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /** Headers clients may send. {@code *} allows all headers. */
    private List<String> allowedHeaders = List.of("*");

    /**
     * Whether the browser should include credentials (cookies, Authorization header).
     *
     * <p>Cannot be {@code true} when {@code allowed-origins} contains {@code *}.
     */
    private boolean allowCredentials = false;

    /** How long (seconds) browsers may cache the pre-flight response. */
    private long maxAge = 3600;
}
