
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.NetworkEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Spring Data JPA repository for [NetworkEntity].
 *
 * Provides CRUD operations and custom query methods for managing monitored networks. Spring Data
 * generates the implementation automatically based on method naming conventions.
 *
 * Extends [JpaRepository] which provides standard methods: save(), findById(), findAll(),
 * delete(), etc. Custom query methods are derived from method names - no implementation code
 * needed.
 */
@Repository
interface NetworkRepository : JpaRepository<NetworkEntity, Long> {

    /**
     * Finds network by exact name match.
     *
     * Network names are unique. Returns Optional to handle case where network doesn't exist.
     * Name is extracted from MQTT topic (e.g., "network/HomeNetwork" → "HomeNetwork").
     *
     * @param name the network name
     * @return Optional containing the network if found, empty otherwise
     */
    fun findByName(name: String): Optional<NetworkEntity>

    /**
     * Check if a network exists by name
     *
     * Useful before creating a new network from MQTT message.
     *
     * @param name the network name to check
     * @return true if network exists, false otherwise
     */
    fun existsByName(name: String): Boolean

    /**
     * Finds all networks with active unresolved alerts.
     *
     * A network has an active alert when [activeAlertId] is not null. Used by alert
     * service to check if network issues have resolved.
     *
     * @return list of networks with active alerts
     */
    fun findByActiveAlertIdIsNotNull(): List<NetworkEntity>

    /**
     * Finds all networks without active alerts.
     *
     * @return list of networks with no active alerts
     */
    fun findByActiveAlertIdIsNull(): List<NetworkEntity>

    /**
     * Find networks by partial name match
     *
     * Case-insensitive search using IgnoreCase.
     */
    fun findByNameContainingIgnoreCase(namePart: String): List<NetworkEntity>
}
