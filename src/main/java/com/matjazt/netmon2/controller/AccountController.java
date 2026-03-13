package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.request.SaveAccountRequest;
import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.entity.AccountTypeEntity;
import com.matjazt.netmon2.service.AccountService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
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
        logger.trace(
                "getAllAccounts: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<AccountDto> dtos = accountService.findAllAccountSummaries();
        logger.trace("getAllAccounts: returning {} accounts", dtos.size());
        return dtos;
    }

    /**
     * GET /api/accounts/paginated?page=0&size=20
     *
     * <p>Get accounts with pagination
     *
     * <p>@RequestParam extracts query parameters from URL
     *
     * <p>defaultValue provides fallback if parameter is missing
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<AccountDto> getAccountsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.trace(
                "getAccountsPaginated: user={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Page<AccountDto> dtoPage = accountService.getAccountSummariesPaginated(page, size);
        logger.trace("getAccountsPaginated: returning {} accounts", dtoPage.getSize());
        return dtoPage;
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
    public ResponseEntity<AccountEntity> getAccountById(@PathVariable Long id) {
        logger.trace(
                "getAccountById: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountService
                .findAccountById(id)
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
    public ResponseEntity<AccountEntity> getAccountByUsername(@PathVariable String username) {
        logger.trace(
                "getAccountByUsername: user={}, targetUsername={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                username);
        return accountService
                .findAccountByUsername(username)
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
        logger.trace(
                "checkAccountExists: user={}, username={}",
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
    public List<AccountEntity> getAccountsByType(@PathVariable String accountTypeName) {
        logger.trace(
                "getAccountsByType: user={}, accountTypeName={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountTypeName);
        return accountService.findAccountsByType(accountTypeName);
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
    public ResponseEntity<AccountEntity> createAccount(@RequestBody SaveAccountRequest request) {
        logger.trace(
                "createAccount: user={}, username={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                request.username());
        AccountEntity saved = accountService.saveAccount(toEntity(request));
        logger.trace("createAccount: created account with id={}", saved.getId());
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
    public ResponseEntity<AccountEntity> updateAccount(
            @PathVariable Long id, @RequestBody SaveAccountRequest request) {
        logger.trace(
                "updateAccount: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        // Verify account exists
        if (!accountService.findAccountById(id).isPresent()) {
            logger.trace("updateAccount: account not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        AccountEntity account = toEntity(request);
        account.setId(id);
        AccountEntity updated = accountService.saveAccount(account);
        logger.trace("updateAccount: updated account with id={}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    private AccountEntity toEntity(SaveAccountRequest request) {
        AccountTypeEntity accountType = new AccountTypeEntity();
        accountType.setId(request.accountTypeId());
        AccountEntity account = new AccountEntity();
        account.setUsername(request.username());
        account.setAccountType(accountType);
        account.setPasswordHash(request.passwordHash());
        account.setFullName(request.fullName());
        account.setEmail(request.email());
        account.setCreatedAt(request.createdAt());
        account.setLastSeen(request.lastSeen());
        return account;
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
        logger.trace(
                "deleteAccount: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        if (!accountService.findAccountById(id).isPresent()) {
            logger.trace("deleteAccount: account not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        accountService.deleteAccount(id);
        logger.trace("deleteAccount: deleted account with id={}", id);
        return ResponseEntity.noContent().build();
    }
}
