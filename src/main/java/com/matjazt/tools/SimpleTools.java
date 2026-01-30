package com.matjazt.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimpleTools {

    private static final DateTimeFormatter DEFAULT_LOCAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatDefault(LocalDateTime timestamp) {
        return timestamp.format(DEFAULT_LOCAL_DATE_TIME_FORMATTER);
    }

    /**
     * Gets a configuration value by checking environment variable first (derived from property
     * name), then system property, then default value.
     *
     * @param propertyName The property name (e.g., "svcwatchdog.udpPingInterval")
     * @param defaultValue The default value if neither env var nor property is set
     * @return The configuration value
     */
    public static String getConfigString(String propertyName, String defaultValue) {
        // Convert property name to environment variable name (e.g., svcwatchdog.udpPingInterval ->
        // SVCWATCHDOG_UDPPINGINTERVAL)
        String envVarName = propertyName.toUpperCase().replace('.', '_');
        String value = System.getenv(envVarName);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(propertyName, defaultValue);
        }
        return value;
    }

    /**
     * Gets a Boolean configuration value by checking environment variable first (derived from
     * property name), then system property, then default value.
     *
     * @param propertyName The property name (e.g., "app.feature.enabled")
     * @param defaultValue The default value if neither env var nor property is set or parsing fails
     * @return The configuration value as Boolean
     */
    public static Boolean getConfigBoolean(String propertyName, Boolean defaultValue) {
        String stringValue = getConfigString(propertyName, null);
        if (stringValue == null) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(stringValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Gets an Integer configuration value by checking environment variable first (derived from
     * property name), then system property, then default value. Supports both decimal and
     * hexadecimal values (with 0x or 0X prefix).
     *
     * @param propertyName The property name (e.g., "app.timeout.seconds")
     * @param defaultValue The default value if neither env var nor property is set or parsing fails
     * @return The configuration value as Integer
     */
    public static Integer getConfigInteger(String propertyName, Integer defaultValue) {
        String stringValue = getConfigString(propertyName, null);
        if (stringValue == null) {
            return defaultValue;
        }
        try {
            // Check for hex prefix
            if (stringValue.startsWith("0x") || stringValue.startsWith("0X")) {
                return Integer.parseInt(stringValue.substring(2), 16);
            } else {
                return Integer.parseInt(stringValue);
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Gets a Long configuration value by checking environment variable first (derived from property
     * name), then system property, then default value. Supports both decimal and hexadecimal values
     * (with 0x or 0X prefix).
     *
     * @param propertyName The property name (e.g., "app.maxSize.bytes")
     * @param defaultValue The default value if neither env var nor property is set or parsing fails
     * @return The configuration value as Long
     */
    public static Long getConfigLong(String propertyName, Long defaultValue) {
        String stringValue = getConfigString(propertyName, null);
        if (stringValue == null) {
            return defaultValue;
        }
        try {
            // Check for hex prefix
            if (stringValue.startsWith("0x") || stringValue.startsWith("0X")) {
                return Long.parseLong(stringValue.substring(2), 16);
            } else {
                return Long.parseLong(stringValue);
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Gets a Double configuration value by checking environment variable first (derived from
     * property name), then system property, then default value.
     *
     * @param propertyName The property name (e.g., "app.threshold.percentage")
     * @param defaultValue The default value if neither env var nor property is set or parsing fails
     * @return The configuration value as Double
     */
    public static Double getConfigDouble(String propertyName, Double defaultValue) {
        String stringValue = getConfigString(propertyName, null);
        if (stringValue == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(stringValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
