package com.matjazt.netmon2.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matjazt.netmon2.entity.AccountTypeEntity;

/**
 * Repository for AccountTypeEntity (roles/permission levels).
 * 
 * This is a simpler repository since account types are typically
 * reference data that doesn't change often.
 */
@Repository
public interface AccountTypeRepository extends JpaRepository<AccountTypeEntity, Long> {

    /**
     * Find account type by name (e.g., "Admin", "Viewer", "MonitoringDevice")
     * 
     * Useful for looking up roles during authentication.
     */
    Optional<AccountTypeEntity> findByName(String name);

    /**
     * Check if an account type exists by name
     */
    boolean existsByName(String name);
}
