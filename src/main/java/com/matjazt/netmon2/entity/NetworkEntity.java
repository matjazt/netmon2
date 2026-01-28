package com.matjazt.netmon2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * JPA Entity representing a monitored network.
 *
 * <p>JPA (Jakarta Persistence API) is similar to Entity Framework in .NET. Entities are POJOs
 * (Plain Old Java Objects) that map to database tables.
 *
 * <p>This entity stores basic information about each monitored network. The network name is
 * extracted from the MQTT topic.
 */
@Entity // Marks this class as a database entity
@Table(name = "network") // Maps to "network" table in database
public class NetworkEntity {

    @Id // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment by database
    private Long id;

    /**
     * Network name (extracted from MQTT topic).
     *
     * <p>For topic "network/MaliGrdi", this would be "MaliGrdi". Unique constraint ensures we don't
     * duplicate networks.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * When this network was first seen.
     *
     * <p>@Column with columnDefinition allows us to use PostgreSQL's TIMESTAMP type.
     */
    @Column(name = "first_seen", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime firstSeen;

    /** When we last received data for this network. */
    @Column(name = "last_seen", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime lastSeen;

    @Column(name = "alerting_delay", nullable = false, columnDefinition = "INTEGER DEFAULT 300")
    private Integer alertingDelay = 300; // in seconds, default 5 minutes

    @Column(name = "email_address", nullable = true, length = 1000)
    private String emailAddress;

    /** If there's an active alert for this network, references the alert ID. */
    @Column(name = "active_alert_id", nullable = true)
    private Long activeAlertId;

    /** JSON configuration for this network. */
    @Column(name = "configuration", nullable = false)
    private String configuration = "{}";

    /** Exponential moving average of reporting interval in seconds. */
    @Column(name = "reporting_interval_ema", nullable = false)
    private Integer reportingIntervalEma = 0;

    /** Timestamp when the network came back online after being down. */
    @Column(name = "back_online_time", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime backOnlineTime;

    // JPA requires a no-argument constructor
    public NetworkEntity() {}

    public NetworkEntity(String name) {
        this.name = name;
        this.firstSeen = LocalDateTime.now(ZoneOffset.UTC);
        this.lastSeen = LocalDateTime.now(ZoneOffset.UTC);
    }

    // Getters and setters - standard Java bean pattern
    // In Java, private fields are accessed via public methods (encapsulation)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Integer getAlertingDelay() {
        return alertingDelay;
    }

    public void setAlertingDelay(Integer alertingDelay) {
        this.alertingDelay = alertingDelay;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    /** Sets the email address for alerts, automatically trimming whitespace. */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress != null ? emailAddress.trim() : null;
    }

    public Long getActiveAlertId() {
        return activeAlertId;
    }

    public void setActiveAlertId(Long activeAlertId) {
        this.activeAlertId = activeAlertId;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Integer getReportingIntervalEma() {
        return reportingIntervalEma;
    }

    public void setReportingIntervalEma(Integer reportingIntervalEma) {
        this.reportingIntervalEma = reportingIntervalEma;
    }

    public LocalDateTime getBackOnlineTime() {
        return backOnlineTime;
    }

    public void setBackOnlineTime(LocalDateTime backOnlineTime) {
        this.backOnlineTime = backOnlineTime;
    }
}
