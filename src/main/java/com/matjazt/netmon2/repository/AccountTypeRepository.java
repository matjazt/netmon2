package com.matjazt.netmon2.repository;

import com.matjazt.netmon2.entity.AccountTypeEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for AccountTypeEntity (roles/permission levels).
 *
 * <p>This is a simpler repository since account types are typically reference data that doesn't
 * change often.
 */
public interface AccountTypeRepository extends JpaRepository<AccountTypeEntity, Integer> {

    /**
     * Find account type by name (e.g., "Admin", "Viewer", "MonitoringDevice")
     *
     * <p>Useful for looking up roles during authentication.
     */
    Optional<AccountTypeEntity> findByName(String name);

    /** Check if an account type exists by name */
    boolean existsByName(String name);
}
