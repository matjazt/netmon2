
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.AccountEntity
import com.matjazt.netmon2.entity.AccountNetworkEntity
import com.matjazt.netmon2.entity.NetworkEntity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for AccountNetworkEntity (join table).
 *
 * Manages the many-to-many relationship between accounts and networks. Determines which accounts
 * can manage which networks.
 */
@Repository
interface AccountNetworkRepository : JpaRepository<AccountNetworkEntity, Long> {

    /**
     * Find all networks an account can manage
     *
     * Returns the join table entries for a specific account.
     */
    fun findByAccount(account: AccountEntity): List<AccountNetworkEntity>

    /**
     * Find all networks an account can manage by account ID
     *
     * Alternative to above - uses ID instead of entity object.
     */
    fun findByAccount_Id(accountId: Long): List<AccountNetworkEntity>

    /** Find all accounts that can manage a network */
    fun findByNetwork(network: NetworkEntity): List<AccountNetworkEntity>

    /** Find all accounts that can manage a network by network ID */
    fun findByNetwork_Id(networkId: Long): List<AccountNetworkEntity>

    /**
     * Check if an account has access to a specific network
     *
     * Useful for authorization checks before allowing actions.
     */
    fun existsByAccount_IdAndNetwork_Id(accountId: Long, networkId: Long): Boolean

    /**
     * CUSTOM QUERY: Get all networks accessible by an account
     *
     * Returns NetworkEntity objects directly instead of join table entries. More convenient when
     * you only need the networks, not the relationship data.
     */
    @Query("SELECT an.network FROM AccountNetworkEntity an WHERE an.account.id = :accountId")
    fun findNetworksByAccountId(@Param("accountId") accountId: Long): List<NetworkEntity>

    /**
     * CUSTOM QUERY: Get all accounts with access to a network
     *
     * Returns AccountEntity objects directly.
     */
    @Query("SELECT an.account FROM AccountNetworkEntity an WHERE an.network.id = :networkId")
    fun findAccountsByNetworkId(@Param("networkId") networkId: Long): List<AccountEntity>

    /**
     * Delete the relationship between an account and a network
     *
     * Spring will automatically generate a DELETE query. Useful for revoking network access from
     * an account.
     */
    fun deleteByAccount_IdAndNetwork_Id(accountId: Long, networkId: Long)
}
