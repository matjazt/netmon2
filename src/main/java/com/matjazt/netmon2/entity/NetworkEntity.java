package com.matjazt.netmon2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Getter
@Setter
@NoArgsConstructor
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

    /** If there's an active alert for this network, references the alert ID. */
    @Column(name = "active_alert_id", nullable = true)
    private Long activeAlertId;

    /** JSON configuration for this network. */
    @Column(name = "configuration", nullable = false)
    private String configuration = "{}";

    /** Timestamp when the network came back online after being down. */
    @Column(name = "back_online_time", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime backOnlineTime;

    public NetworkEntity(String name) {
        this.name = name;
        this.firstSeen = LocalDateTime.now(ZoneOffset.UTC);
        this.lastSeen = LocalDateTime.now(ZoneOffset.UTC);
    }

    @Override
    public String toString() {
        return name + " (id: " + id + ")";
    }
}
