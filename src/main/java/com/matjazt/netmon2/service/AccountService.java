package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.request.SaveAccountRequest;
import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.mapper.AccountMapper;
import com.matjazt.netmon2.repository.AccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * Find account by ID
     *
     * <p>Optional avoids NullPointerException - you must check if value exists.
     */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountEntity> findAccountById(Long id) {
        log.trace(
                "findAccountById: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountRepository.findById(id);
    }

    /** Get all accounts (admin only) */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountEntity> findAllAccounts() {
        log.trace(
                "findAllAccounts: apiUser={}",
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
        log.trace(
                "saveAccount: apiUser={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                account.getUsername());
        return accountRepository.save(account);
    }

    /** Delete an account */
    @Transactional
    @PreAuthorize("hasAnyRole('admin')")
    public void deleteAccount(Long id) {
        log.trace(
                "deleteAccount: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        accountRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY EXAMPLES ==========

    /** Find account by username */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountEntity> findAccountByUsername(String username) {
        log.trace(
                "findAccountByUsername: apiUser={}, targetUsername={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountRepository.findByUsername(username);
    }

    /** Check if account exists by username */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean accountExistsByUsername(String username) {
        log.trace(
                "accountExistsByUsername: apiUser={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountRepository.existsByUsername(username);
    }

    /** Check if email exists */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean emailExists(String email) {
        log.trace(
                "emailExists: apiUser={}, email={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                email);
        return accountRepository.existsByEmail(email);
    }

    /** Find accounts by account type */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountEntity> findAccountsByType(String accountTypeName) {
        log.trace(
                "findAccountsByType: apiUser={}, accountTypeName={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountTypeName);
        return accountRepository.findByAccountType_Name(accountTypeName);
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all accounts as DTOs */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountDto> findAllAccountSummaries() {
        log.trace(
                "findAllAccountSummaries: apiUser={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<AccountEntity> entities = accountRepository.findAll();
        var dtos = accountMapper.toDtos(entities);
        log.trace("findAllAccountSummaries: returning {} accounts", dtos.size());
        return dtos;
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
    public AccountDto saveAccountAndReturnDto(SaveAccountRequest request, Long id) {
        AccountEntity account = accountMapper.toEntity(request);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        if (id != null) account.setId(id);
        return accountMapper.toDto(saveAccount(account));
    }
}
