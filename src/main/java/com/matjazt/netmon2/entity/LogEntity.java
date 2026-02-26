package com.matjazt.netmon2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a log entry for network/device events.
 *
 * <p>This entity captures log messages that reference NetworkEntity or DeviceEntity, allowing
 * correlation between application logs and monitored entities.
 */
@Entity
@Table(name = "log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Timestamp when the log entry was created. */
    @Column(name = "timestamp", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    /**
     * Log level as integer (matches Logback levels).
     *
     * <p>Typical values: TRACE=5000, DEBUG=10000, INFO=20000, WARN=30000, ERROR=40000
     */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** Origin of the log message (logger name). */
    @Column(name = "origin", nullable = false, length = 500)
    private String origin;

    /** The formatted log message. */
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** Required reference to the network. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private NetworkEntity network;

    /** Optional reference to a device (if log message references a device). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = true)
    private DeviceEntity device;
}
