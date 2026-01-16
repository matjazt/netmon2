package com.matjazt.netmon2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.entity.AccountNetworkEntity;
import com.matjazt.netmon2.entity.NetworkEntity;

/**
 * Repository for AccountNetworkEntity (join table).
 * 
 * Manages the many-to-many relationship between accounts and networks.
 * Determines which accounts can manage which networks.
 */
@Repository
public interface AccountNetworkRepository extends JpaRepository<AccountNetworkEntity, Long> {

    /**
     * Find all networks an account can manage
     * 
     * Returns the join table entries for a specific account.
     */
    List<AccountNetworkEntity> findByAccount(AccountEntity account);

    /**
     * Find all networks an account can manage by account ID
     * 
     * Alternative to above - uses ID instead of entity object.
     */
    List<AccountNetworkEntity> findByAccount_Id(Long accountId);

    /**
     * Find all accounts that can manage a network
     */
    List<AccountNetworkEntity> findByNetwork(NetworkEntity network);

    /**
     * Find all accounts that can manage a network by network ID
     */
    List<AccountNetworkEntity> findByNetwork_Id(Long networkId);

    /**
     * Check if an account has access to a specific network
     * 
     * Useful for authorization checks before allowing actions.
     */
    boolean existsByAccount_IdAndNetwork_Id(Long accountId, Long networkId);

    /**
     * CUSTOM QUERY: Get all networks accessible by an account
     * 
     * Returns NetworkEntity objects directly instead of join table entries.
     * More convenient when you only need the networks, not the relationship data.
     */
    @Query("SELECT an.network FROM AccountNetworkEntity an WHERE an.account.id = :accountId")
    List<NetworkEntity> findNetworksByAccountId(@Param("accountId") Long accountId);

    /**
     * CUSTOM QUERY: Get all accounts with access to a network
     * 
     * Returns AccountEntity objects directly.
     */
    @Query("SELECT an.account FROM AccountNetworkEntity an WHERE an.network.id = :networkId")
    List<AccountEntity> findAccountsByNetworkId(@Param("networkId") Long networkId);

    /**
     * Delete the relationship between an account and a network
     * 
     * Spring will automatically generate a DELETE query.
     * Useful for revoking network access from an account.
     */
    void deleteByAccount_IdAndNetwork_Id(Long accountId, Long networkId);
}
