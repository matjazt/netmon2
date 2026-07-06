package com.matjazt.netmon2.service;

import com.matjazt.netmon2.config.ApplicationTransactional;
import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.request.SaveAccountRequest;
import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.entity.AccountTypeEntity;
import com.matjazt.netmon2.mapper.AccountMapper;
import com.matjazt.netmon2.repository.AccountRepository;
import com.matjazt.netmon2.repository.AccountTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
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
    private final AccountTypeRepository accountTypeRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * Find account by ID
     *
     * <p>Optional avoids NullPointerException - you must check if value exists.
     */
    public Optional<AccountEntity> findAccountById(Long id) {
        log.trace(
                "findAccountById: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountRepository.findById(id);
    }

    /** Get all accounts (admin only) */
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
    @ApplicationTransactional
    public AccountEntity saveAccount(AccountEntity account) {
        log.trace(
                "saveAccount: apiUser={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                account.getUsername());
        return accountRepository.save(account);
    }

    /** Delete an account */
    @ApplicationTransactional
    public void deleteAccount(Long id) {
        log.trace(
                "deleteAccount: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        accountRepository.deleteById(id);
    }

    /** Find account by username */
    public Optional<AccountEntity> findAccountByUsername(String username) {
        log.trace(
                "findAccountByUsername: apiUser={}, targetUsername={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountRepository.findByUsername(username);
    }

    /** Check if account exists by username */
    public boolean accountExistsByUsername(String username) {
        log.trace(
                "accountExistsByUsername: apiUser={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountRepository.existsByUsername(username);
    }

    /** Check if email exists */
    public boolean emailExists(String email) {
        log.trace(
                "emailExists: apiUser={}, email={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                email);
        return accountRepository.existsByEmail(email);
    }

    /** Find accounts by account type */
    public List<AccountEntity> findAccountsByType(String accountTypeName) {
        log.trace(
                "findAccountsByType: apiUser={}, accountTypeName={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountTypeName);
        return accountRepository.findByAccountType_Name(accountTypeName);
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all accounts as DTOs */
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

    public Optional<AccountDto> findAccountDtoById(Long id) {
        return findAccountById(id).map(accountMapper::toDto);
    }

    public Optional<AccountDto> findAccountDtoByUsername(String username) {
        return findAccountByUsername(username).map(accountMapper::toDto);
    }

    public List<AccountDto> findAccountDtosByType(String accountTypeName) {
        return accountMapper.toDtos(findAccountsByType(accountTypeName));
    }

    @ApplicationTransactional
    public AccountDto saveAccountAndReturnDto(SaveAccountRequest request, Long id) {
        AccountTypeEntity accountType =
                accountTypeRepository
                        .findById(request.accountTypeId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Account type not found: "
                                                        + request.accountTypeId()));
        AccountEntity account = accountMapper.toEntity(request);
        account.setAccountType(accountType);

        if (request.password() == null || request.password().isEmpty()) {
            if (id != null) {
                String existingHash =
                        accountRepository
                                .findById(id)
                                .orElseThrow(
                                        () ->
                                                new NoSuchElementException(
                                                        "Account not found: " + id))
                                .getPasswordHash();
                account.setPasswordHash(existingHash);
            } else {
                throw new IllegalArgumentException("Password is required for new accounts");
            }
        } else {
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (id != null) {
            account.setId(id);
        }
        return accountMapper.toDto(saveAccount(account));
    }
}
