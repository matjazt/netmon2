package com.matjazt.netmon2.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matjazt.netmon2.entity.NetworkEntity;

/**
 * Repository for NetworkEntity.
 * 
 * Manages monitored networks.
 */
@Repository
public interface NetworkRepository extends JpaRepository<NetworkEntity, Long> {

    /**
     * Find network by name
     * 
     * Network name is unique, so we return Optional.
     * The name is extracted from MQTT topic (e.g., "MaliGrdi").
     */
    Optional<NetworkEntity> findByName(String name);

    /**
     * Check if a network exists by name
     * 
     * Useful before creating a new network from MQTT message.
     */
    boolean existsByName(String name);

    /**
     * Find networks that have an active alert
     * 
     * activeAlertId is not null means there's an unresolved alert.
     */
    java.util.List<NetworkEntity> findByActiveAlertIdIsNotNull();

    /**
     * Find networks without active alerts
     */
    java.util.List<NetworkEntity> findByActiveAlertIdIsNull();

    /**
     * Find networks by partial name match
     * 
     * Case-insensitive search using IgnoreCase.
     */
    java.util.List<NetworkEntity> findByNameContainingIgnoreCase(String namePart);
}
