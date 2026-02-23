package com.matjazt.netmon2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA Entity representing an alert (alert) in the system.
 *
 * <p>Alerts are triggered when networks or devices go down, or when unauthorized devices are
 * detected.
 */
@Entity
@Table(name = "alert")
@Getter
@Setter
@NoArgsConstructor
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** When this alert was triggered. */
    @Column(name = "timestamp", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    /** The network this alert is associated with. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private NetworkEntity network;

    /**
     * The device this alert is associated with (optional).
     *
     * <p>Null for network-level alerts.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = true)
    private DeviceEntity device;

    /**
     * The type of alert.
     *
     * <p>Stored as integer matching alert_type.id for referential integrity.
     */
    @Column(name = "alert_type_id", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private AlertType alertType;

    /**
     * Reference to AlertTypeEntity for OpenJPA foreign key validation only.
     *
     * <p>Not used in runtime code - insertable/updatable=false ensures enum field controls the
     * value.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_type_id", insertable = false, updatable = false)
    private AlertTypeEntity alertTypeRef;

    /** Human-readable alert message. */
    @Column(name = "message", nullable = true, length = 500)
    private String message;

    /** When this alert was closed/resolved (optional). */
    @Column(name = "closure_timestamp", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime closureTimestamp;

    /** Last time a notification was sent for this alert. */
    @Column(name = "last_notification_timestamp", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime lastNotificationTimestamp;

    public AlertEntity(
            LocalDateTime timestamp,
            NetworkEntity network,
            DeviceEntity device,
            AlertType alertType,
            String message) {
        this.timestamp = timestamp;
        this.network = network;
        this.device = device;
        this.alertType = alertType;
        this.message = message;
        this.lastNotificationTimestamp = timestamp;
    }
}
