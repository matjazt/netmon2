package com.matjazt.netmon2.repository;

import com.matjazt.netmon2.entity.NetworkEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for NetworkEntity.
 *
 * <p>Manages monitored networks.
 */
@Repository
public interface NetworkRepository extends JpaRepository<NetworkEntity, Long> {

    /**
     * Find network by name
     *
     * <p>Network name is unique, so we return Optional. The name is extracted from MQTT topic
     * (e.g., "MaliGrdi").
     */
    Optional<NetworkEntity> findByName(String name);

    /**
     * Check if a network exists by name
     *
     * <p>Useful before creating a new network from MQTT message.
     */
    boolean existsByName(String name);

    /**
     * Find networks that have an active alert
     *
     * <p>activeAlertId is not null means there's an unresolved alert.
     */
    java.util.List<NetworkEntity> findByActiveAlertIdIsNotNull();

    /** Find networks without active alerts */
    java.util.List<NetworkEntity> findByActiveAlertIdIsNull();

    /**
     * Find networks by partial name match
     *
     * <p>Case-insensitive search using IgnoreCase.
     */
    java.util.List<NetworkEntity> findByNameContainingIgnoreCase(String namePart);
}
