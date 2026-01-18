package com.matjazt.netmon2.repository;

import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.entity.AccountNetworkEntity;
import com.matjazt.netmon2.entity.NetworkEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AccountNetworkEntity (join table).
 *
 * <p>Manages the many-to-many relationship between accounts and networks. Determines which accounts
 * can manage which networks.
 */
@Repository
public interface AccountNetworkRepository extends JpaRepository<AccountNetworkEntity, Long> {

    /**
     * Find all networks an account can manage
     *
     * <p>Returns the join table entries for a specific account.
     */
    List<AccountNetworkEntity> findByAccount(AccountEntity account);

    /**
     * Find all networks an account can manage by account ID
     *
     * <p>Alternative to above - uses ID instead of entity object.
     */
    List<AccountNetworkEntity> findByAccount_Id(Long accountId);

    /** Find all accounts that can manage a network */
    List<AccountNetworkEntity> findByNetwork(NetworkEntity network);

    /** Find all accounts that can manage a network by network ID */
    List<AccountNetworkEntity> findByNetwork_Id(Long networkId);

    /**
     * Check if an account has access to a specific network
     *
     * <p>Useful for authorization checks before allowing actions.
     */
    boolean existsByAccount_IdAndNetwork_Id(Long accountId, Long networkId);

    /**
     * CUSTOM QUERY: Get all networks accessible by an account
     *
     * <p>Returns NetworkEntity objects directly instead of join table entries. More convenient when
     * you only need the networks, not the relationship data.
     */
    @Query("SELECT an.network FROM AccountNetworkEntity an WHERE an.account.id = :accountId")
    List<NetworkEntity> findNetworksByAccountId(@Param("accountId") Long accountId);

    /**
     * CUSTOM QUERY: Get all accounts with access to a network
     *
     * <p>Returns AccountEntity objects directly.
     */
    @Query("SELECT an.account FROM AccountNetworkEntity an WHERE an.network.id = :networkId")
    List<AccountEntity> findAccountsByNetworkId(@Param("networkId") Long networkId);

    /**
     * Delete the relationship between an account and a network
     *
     * <p>Spring will automatically generate a DELETE query. Useful for revoking network access from
     * an account.
     */
    void deleteByAccount_IdAndNetwork_Id(Long accountId, Long networkId);
}
