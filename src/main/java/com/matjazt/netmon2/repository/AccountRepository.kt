
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.AccountEntity

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

import java.time.LocalDateTime
import java.util.Optional

/**
 * Spring Data JPA Repository for AccountEntity.
 *
 * By extending JpaRepository, we automatically get methods like:
 *
 * - save(entity) - insert or update
 * - findById(id) - find by primary key
 * - findAll() - get all records
 * - deleteById(id) - delete by primary key
 * - count() - count all records
 *
 * @Repository marks this as a Data Access Object (DAO) component. Spring will automatically
 * create an implementation at runtime!
 */
@Repository
interface AccountRepository : JpaRepository<AccountEntity, Long> {

    fun findByUsername(username: String): AccountEntity?

    @Cacheable(cacheNames = ["userDetailsCache"], key = "#username", sync = true)
    @Query("SELECT a FROM AccountEntity a WHERE a.username = :username")
    fun cachedFindByUsername(@Param("username") username: String): AccountEntity?

    fun findByUsernameAndEmail(username: String, email: String): AccountEntity?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun findByFullNameContaining(name: String): List<AccountEntity>

    fun findByAccountType_Name(accountTypeName: String): List<AccountEntity>

    fun findByCreatedAtAfter(date: LocalDateTime): List<AccountEntity>

    @Query("SELECT a FROM AccountEntity a WHERE a.username = :username AND a.lastSeen > :sinceDate")
    fun findActiveUser(
        @Param("username") username: String,
        @Param("sinceDate") sinceDate: LocalDateTime
    ): AccountEntity?

    @Query(
        value = "SELECT * FROM account WHERE last_seen IS NOT NULL ORDER BY last_seen DESC LIMIT :limit",
        nativeQuery = true
    )
    fun findRecentlyActiveAccounts(@Param("limit") limit: Int): List<AccountEntity>

    @Query("SELECT a FROM AccountEntity a JOIN FETCH a.accountType WHERE a.id = :id")
    fun findByIdWithAccountType(@Param("id") id: Long): AccountEntity?

    fun countByAccountType_Name(accountTypeName: String): Long
}
