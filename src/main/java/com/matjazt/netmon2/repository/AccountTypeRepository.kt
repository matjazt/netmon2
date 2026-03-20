
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.AccountTypeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repository for AccountTypeEntity (roles/permission levels).
 *
 * This is a simpler repository since account types are typically reference data that doesn't
 * change often.
 */
@Repository
interface AccountTypeRepository : JpaRepository<AccountTypeEntity, Integer> {

    /**
     * Find account type by name (e.g., "Admin", "Viewer", "MonitoringDevice")
     *
     * Useful for looking up roles during authentication.
     */
    fun findByName(name: String): AccountTypeEntity?

    /** Check if an account type exists by name */
    fun existsByName(name: String): Boolean
}
