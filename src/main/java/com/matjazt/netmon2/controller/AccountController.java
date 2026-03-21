package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.request.SaveAccountRequest;
import com.matjazt.netmon2.service.AccountService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for managing Account entities.
 *
 * <p>@RestController combines @Controller and @ResponseBody
 *
 * <p>Provides CRUD endpoints for user accounts (admin only).
 */
@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasAnyRole('admin', 'system')")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /**
     * GET /api/accounts
     *
     * <p>Get all accounts Returns 200 OK with JSON array of accounts
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountDto> getAllAccounts() {
        log.trace(
                "getAllAccounts: apiUser={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<AccountDto> dtos = accountService.findAllAccountSummaries();
        log.trace("getAllAccounts: returning {} accounts", dtos.size());
        return dtos;
    }

    /**
     * GET /api/accounts/5
     *
     * <p>Get account by ID
     *
     * <p>@PathVariable extracts {id} from URL path
     *
     * <p>Returns:
     *
     * <ul>
     *   <li>200 OK with account JSON if found
     *   <li>404 Not Found if account doesn't exist
     * </ul>
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        log.trace(
                "getAccountById: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountService
                .findAccountDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/accounts/username/john
     *
     * <p>Find account by username
     *
     * <p>Username is part of the URL path
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<AccountDto> getAccountByUsername(@PathVariable String username) {
        log.trace(
                "getAccountByUsername: apiUser={}, targetUsername={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountService
                .findAccountDtoByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/accounts/me — return the currently authenticated account. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountDto> getCurrentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.trace("getCurrentAccount: apiUser={}", username);
        return accountService
                .findAccountDtoByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/accounts/exists?username=john
     *
     * <p>Check if account exists (returns boolean)
     */
    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean checkAccountExists(@RequestParam String username) {
        log.trace(
                "checkAccountExists: apiUser={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountService.accountExistsByUsername(username);
    }

    /**
     * GET /api/accounts/type/admin
     *
     * <p>Get accounts by account type
     */
    @GetMapping("/type/{accountTypeName}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountDto> getAccountsByType(@PathVariable String accountTypeName) {
        log.trace(
                "getAccountsByType: apiUser={}, accountTypeName={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountTypeName);
        return accountService.findAccountDtosByType(accountTypeName);
    }

    // ========== POST ENDPOINTS (create new resources) ==========

    /**
     * POST /api/accounts
     *
     * <p>Create a new account
     *
     * <p>@RequestBody deserializes JSON from request body to AccountEntity
     *
     * <p>Returns 201 Created with the saved account (including generated ID)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin')")
    public ResponseEntity<AccountDto> createAccount(@RequestBody SaveAccountRequest request) {
        log.trace(
                "createAccount: apiUser={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                request.username());
        AccountDto saved = accountService.saveAccountAndReturnDto(request, null);
        log.trace("createAccount: created account with id={}", saved.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ========== PUT ENDPOINTS (update existing resources) ==========

    /**
     * PUT /api/accounts/5
     *
     * <p>Update an existing account
     *
     * <p>ID in path + full entity in body
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin')")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable Long id, @RequestBody SaveAccountRequest request) {
        log.trace(
                "updateAccount: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        // Verify account exists
        if (!accountService.findAccountDtoById(id).isPresent()) {
            log.trace("updateAccount: account not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        AccountDto updated = accountService.saveAccountAndReturnDto(request, id);
        log.trace("updateAccount: updated account with id={}", updated.id());
        return ResponseEntity.ok(updated);
    }

    // ========== DELETE ENDPOINTS (remove resources) ==========

    /**
     * DELETE /api/accounts/5
     *
     * <p>Delete an account
     *
     * <p>Returns 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        log.trace(
                "deleteAccount: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        if (!accountService.findAccountDtoById(id).isPresent()) {
            log.trace("deleteAccount: account not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        accountService.deleteAccount(id);
        log.trace("deleteAccount: deleted account with id={}", id);
        return ResponseEntity.noContent().build();
    }
}
