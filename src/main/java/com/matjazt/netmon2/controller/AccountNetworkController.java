package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.AccountNetworkDto;
import com.matjazt.netmon2.dto.request.SaveAccountNetworkRequest;
import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.entity.AccountNetworkEntity;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.service.AccountNetworkService;

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
 * REST Controller for managing AccountNetwork relationships.
 *
 * <p>@RestController combines @Controller and @ResponseBody
 *
 * <p>Provides CRUD endpoints for managing which accounts have access to which networks.
 */
@RestController
@RequestMapping("/api/account-networks")
@PreAuthorize("hasAnyRole('admin', 'system')")
@Slf4j
@RequiredArgsConstructor
public class AccountNetworkController {

    private final AccountNetworkService accountNetworkService;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /**
     * GET /api/account-networks
     *
     * <p>Get all account-network relationships Returns 200 OK with JSON array
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountNetworkDto> getAllAccountNetworks() {
        logger.trace(
                "getAllAccountNetworks: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<AccountNetworkDto> dtos = accountNetworkService.findAllSummaries();
        logger.trace("getAllAccountNetworks: returning {} relationships", dtos.size());
        return dtos;
    }

    /**
     * GET /api/account-networks/paginated?page=0&size=20
     *
     * <p>Get account-network relationships with pagination
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<AccountNetworkDto> getAccountNetworksPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.trace(
                "getAccountNetworksPaginated: user={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Page<AccountNetworkDto> dtoPage = accountNetworkService.getSummariesPaginated(page, size);
        logger.trace("getAccountNetworksPaginated: returning {} relationships", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET /api/account-networks/5
     *
     * <p>Get account-network relationship by ID
     *
     * <p>Returns:
     *
     * <ul>
     *   <li>200 OK with relationship JSON if found
     *   <li>404 Not Found if relationship doesn't exist
     * </ul>
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<AccountNetworkEntity> getAccountNetworkById(@PathVariable Long id) {
        logger.trace(
                "getAccountNetworkById: user={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountNetworkService
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/account-networks/account/5
     *
     * <p>Get all networks accessible by an account
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountNetworkDto> getNetworksByAccount(@PathVariable Long accountId) {
        logger.trace(
                "getNetworksByAccount: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId);
        List<AccountNetworkDto> dtos = accountNetworkService.getByAccountId(accountId);
        logger.trace("getNetworksByAccount: returning {} relationships", dtos.size());
        return dtos;
    }

    /**
     * GET /api/account-networks/network/5
     *
     * <p>Get all accounts with access to a network
     */
    @GetMapping("/network/{networkId}")
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public List<AccountNetworkDto> getAccountsByNetwork(@PathVariable Long networkId) {
        logger.trace(
                "getAccountsByNetwork: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        List<AccountNetworkDto> dtos = accountNetworkService.getByNetworkId(networkId);
        logger.trace("getAccountsByNetwork: returning {} relationships", dtos.size());
        return dtos;
    }

    /**
     * GET /api/account-networks/networks-by-account/5
     *
     * <p>Get network entities accessible by an account (direct network objects)
     */
    @GetMapping("/networks-by-account/{accountId}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkEntity> getNetworkEntitiesByAccount(@PathVariable Long accountId) {
        logger.trace(
                "getNetworkEntitiesByAccount: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId);
        var networks = accountNetworkService.getNetworksByAccountId(accountId);
        logger.trace("getNetworkEntitiesByAccount: returning {} networks", networks.size());
        return networks;
    }

    /**
     * GET /api/account-networks/accounts-by-network/5
     *
     * <p>Get account entities with access to a network (direct account objects)
     */
    @GetMapping("/accounts-by-network/{networkId}")
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public List<AccountEntity> getAccountEntitiesByNetwork(@PathVariable Long networkId) {
        logger.trace(
                "getAccountEntitiesByNetwork: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        var accounts = accountNetworkService.getAccountsByNetworkId(networkId);
        logger.trace("getAccountEntitiesByNetwork: returning {} accounts", accounts.size());
        return accounts;
    }

    /**
     * GET /api/account-networks/has-access?accountId=5&networkId=10
     *
     * <p>Check if an account has access to a network (returns boolean)
     */
    @GetMapping("/has-access")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean checkAccess(@RequestParam Long accountId, @RequestParam Long networkId) {
        logger.trace(
                "checkAccess: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);
        return accountNetworkService.hasAccess(accountId, networkId);
    }

    // ========== POST ENDPOINTS (create new resources) ==========

    /**
     * POST /api/account-networks
     *
     * <p>Create a new account-network relationship
     *
     * <p>@RequestBody deserializes JSON from request body to AccountNetworkEntity
     *
     * <p>Returns 201 Created with the saved relationship (including generated ID)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<AccountNetworkEntity> createAccountNetwork(
            @RequestBody SaveAccountNetworkRequest request) {
        logger.trace(
                "createAccountNetwork: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                request.accountId(),
                request.networkId());
        AccountNetworkEntity saved = accountNetworkService.save(toEntity(request));
        logger.trace("createAccountNetwork: created relationship with id={}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST /api/account-networks/grant-access?accountId=5&networkId=10
     *
     * <p>Grant access to a network for an account
     *
     * <p>Convenience method that doesn't require full entity in body
     */
    @PostMapping("/grant-access")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<AccountNetworkEntity> grantAccess(
            @RequestParam Long accountId, @RequestParam Long networkId) {
        logger.trace(
                "grantAccess: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);
        try {
            AccountNetworkEntity saved = accountNetworkService.grantAccess(accountId, networkId);
            logger.trace("grantAccess: created relationship with id={}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            logger.trace("grantAccess: failed - {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== PUT ENDPOINTS (update existing resources) ==========

    /**
     * PUT /api/account-networks/5
     *
     * <p>Update an existing account-network relationship
     *
     * <p>ID in path + full entity in body
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<AccountNetworkEntity> updateAccountNetwork(
            @PathVariable Long id, @RequestBody SaveAccountNetworkRequest request) {
        logger.trace(
                "updateAccountNetwork: user={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        // Verify relationship exists
        if (!accountNetworkService.findById(id).isPresent()) {
            logger.trace("updateAccountNetwork: relationship not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        AccountNetworkEntity entity = toEntity(request);
        entity.setId(id);
        AccountNetworkEntity updated = accountNetworkService.save(entity);
        logger.trace("updateAccountNetwork: updated relationship with id={}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    private AccountNetworkEntity toEntity(SaveAccountNetworkRequest request) {
        AccountEntity account = new AccountEntity();
        account.setId(request.accountId());
        NetworkEntity network = new NetworkEntity();
        network.setId(request.networkId());
        return new AccountNetworkEntity(account, network);
    }

    // ========== DELETE ENDPOINTS (remove resources) ==========

    /**
     * DELETE /api/account-networks/5
     *
     * <p>Delete an account-network relationship
     *
     * <p>Returns 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<Void> deleteAccountNetwork(@PathVariable Long id) {
        logger.trace(
                "deleteAccountNetwork: user={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        if (!accountNetworkService.findById(id).isPresent()) {
            logger.trace("deleteAccountNetwork: relationship not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        accountNetworkService.delete(id);
        logger.trace("deleteAccountNetwork: deleted relationship with id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/account-networks/revoke-access?accountId=5&networkId=10
     *
     * <p>Revoke access to a network for an account
     *
     * <p>Convenience method using query parameters
     */
    @DeleteMapping("/revoke-access")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<Void> revokeAccess(
            @RequestParam Long accountId, @RequestParam Long networkId) {
        logger.trace(
                "revokeAccess: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);
        accountNetworkService.revokeAccess(accountId, networkId);
        logger.trace(
                "revokeAccess: revoked access for accountId={}, networkId={}",
                accountId,
                networkId);
        return ResponseEntity.noContent().build();
    }
}
