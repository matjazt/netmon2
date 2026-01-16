package com.matjazt.netmon2.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matjazt.netmon2.entity.AccountEntity;

/**
 * Spring Data JPA Repository for AccountEntity.
 * 
 * By extending JpaRepository, we automatically get methods like:
 * - save(entity) - insert or update
 * - findById(id) - find by primary key
 * - findAll() - get all records
 * - deleteById(id) - delete by primary key
 * - count() - count all records
 * 
 * @Repository marks this as a Data Access Object (DAO) component.
 *             Spring will automatically create an implementation at runtime!
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    /**
     * DERIVED QUERY METHOD - Spring parses the method name and creates the query!
     * 
     * Method naming convention:
     * findBy + PropertyName + Condition
     * 
     * This generates: SELECT * FROM account WHERE username = ?
     */
    Optional<AccountEntity> findByUsername(String username);

    /**
     * DERIVED QUERY with multiple conditions using AND
     * 
     * Generates: SELECT * FROM account WHERE username = ? AND email = ?
     */
    Optional<AccountEntity> findByUsernameAndEmail(String username, String email);

    /**
     * DERIVED QUERY to check existence
     * 
     * Returns true if a record exists with the given username.
     * More efficient than findByUsername because it doesn't load the entity.
     */
    boolean existsByUsername(String username);

    /**
     * DERIVED QUERY to check if email exists
     * Useful for validation during registration.
     */
    boolean existsByEmail(String email);

    /**
     * DERIVED QUERY with LIKE for partial matching
     * 
     * Example: findByFullNameContaining("John") finds "John Doe", "Johnny Smith",
     * etc.
     * The % wildcards are automatically added by Spring.
     */
    java.util.List<AccountEntity> findByFullNameContaining(String name);

    /**
     * DERIVED QUERY with relationship navigation
     * 
     * Navigate through the accountType relationship using underscore notation.
     * Generates: SELECT a FROM Account a WHERE a.accountType.name = ?
     */
    java.util.List<AccountEntity> findByAccountType_Name(String accountTypeName);

    /**
     * DERIVED QUERY with date comparison
     * 
     * Find accounts created after a specific date.
     * Other options: Before, Between, GreaterThanEqual, LessThanEqual
     */
    java.util.List<AccountEntity> findByCreatedAtAfter(LocalDateTime date);

    /**
     * CUSTOM JPQL QUERY using @Query annotation
     * 
     * Use this when derived queries get too complex.
     * JPQL is similar to SQL but uses entity names and properties instead of
     * table/column names.
     * 
     * :username is a named parameter (safer than positional ?1, ?2)
     */
    @Query("SELECT a FROM AccountEntity a WHERE a.username = :username AND a.lastSeen > :sinceDate")
    Optional<AccountEntity> findActiveUser(@Param("username") String username,
            @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * NATIVE SQL QUERY using @Query with nativeQuery = true
     * 
     * Use this when you need database-specific SQL features.
     * Returns entity objects by default.
     */
    @Query(value = "SELECT * FROM account WHERE last_seen IS NOT NULL ORDER BY last_seen DESC LIMIT :limit", nativeQuery = true)
    java.util.List<AccountEntity> findRecentlyActiveAccounts(@Param("limit") int limit);

    /**
     * JPQL Query with JOIN FETCH for eager loading
     * 
     * Solves the N+1 query problem by loading related entities in one query.
     * Without this, accessing account.getAccountType() would trigger separate
     * queries.
     */
    @Query("SELECT a FROM AccountEntity a JOIN FETCH a.accountType WHERE a.id = :id")
    Optional<AccountEntity> findByIdWithAccountType(@Param("id") Long id);

    /**
     * Count accounts by type using derived query
     */
    long countByAccountType_Name(String accountTypeName);
}
