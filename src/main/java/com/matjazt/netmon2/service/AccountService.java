package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.mapper.AccountMapper;
import com.matjazt.netmon2.repository.AccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Account entities.
 *
 * <p>Provides CRUD operations and business logic for user accounts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * Find account by ID
     *
     * <p>Optional avoids NullPointerException - you must check if value exists.
     */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountEntity> findAccountById(Long id) {
        logger.trace(
                "findAccountById: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountRepository.findById(id);
    }

    /** Get all accounts (admin only) */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountEntity> findAllAccounts() {
        logger.trace(
                "findAllAccounts: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        return accountRepository.findAll();
    }

    /**
     * Save a new account or update existing one
     *
     * <p>save() does INSERT if ID is null, UPDATE if ID exists.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public AccountEntity saveAccount(AccountEntity account) {
        logger.trace(
                "saveAccount: user={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                account.getUsername());
        return accountRepository.save(account);
    }

    /** Delete an account */
    @Transactional
    @PreAuthorize("hasAnyRole('admin')")
    public void deleteAccount(Long id) {
        logger.trace(
                "deleteAccount: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        accountRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY EXAMPLES ==========

    /** Find account by username */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountEntity> findAccountByUsername(String username) {
        logger.trace(
                "findAccountByUsername: user={}, targetUsername={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountRepository.findByUsername(username);
    }

    /** Check if account exists by username */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean accountExistsByUsername(String username) {
        logger.trace(
                "accountExistsByUsername: user={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountRepository.existsByUsername(username);
    }

    /** Check if email exists */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean emailExists(String email) {
        logger.trace(
                "emailExists: user={}, email={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                email);
        return accountRepository.existsByEmail(email);
    }

    /** Find accounts by account type */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountEntity> findAccountsByType(String accountTypeName) {
        logger.trace(
                "findAccountsByType: user={}, accountTypeName={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountTypeName);
        return accountRepository.findByAccountType_Name(accountTypeName);
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all accounts as DTOs */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountDto> findAllAccountSummaries() {
        logger.trace(
                "findAllAccountSummaries: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<AccountEntity> entities = accountRepository.findAll();
        var dtos = accountMapper.toDtos(entities);
        logger.trace("findAllAccountSummaries: returning {} accounts", dtos.size());
        return dtos;
    }

    /**
     * Pagination - get accounts page by page
     *
     * <p>Pageable defines page number, size, and sorting. Page contains results + metadata (total
     * pages, total elements, etc.)
     */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<AccountDto> getAccountSummariesPaginated(int page, int size) {
        logger.trace(
                "getAccountSummariesPaginated: user={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<AccountEntity> entityPage = accountRepository.findAll(pageable);
        var dtoPage = accountMapper.toDtoPage(entityPage);
        logger.trace("getAccountSummariesPaginated: returning {} accounts", dtoPage.getSize());
        return dtoPage;
    }

    // ========== DTO SINGLE-RECORD METHODS ==========

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountDto> findAccountDtoById(Long id) {
        return findAccountById(id).map(accountMapper::toDto);
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountDto> findAccountDtoByUsername(String username) {
        return findAccountByUsername(username).map(accountMapper::toDto);
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountDto> findAccountDtosByType(String accountTypeName) {
        return accountMapper.toDtos(findAccountsByType(accountTypeName));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public AccountDto saveAccountAndReturnDto(AccountEntity account) {
        return accountMapper.toDto(saveAccount(account));
    }
}
