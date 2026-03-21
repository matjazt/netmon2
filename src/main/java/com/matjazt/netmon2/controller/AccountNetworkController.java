package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.AccountNetworkDto;
import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.request.SaveAccountNetworkRequest;
import com.matjazt.netmon2.service.AccountNetworkService;

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
        log.trace(
                "getAllAccountNetworks: apiUser={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<AccountNetworkDto> dtos = accountNetworkService.findAllSummaries();
        log.trace("getAllAccountNetworks: returning {} relationships", dtos.size());
        return dtos;
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
    public ResponseEntity<AccountNetworkDto> getAccountNetworkById(@PathVariable Long id) {
        log.trace(
                "getAccountNetworkById: apiUser={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountNetworkService
                .findDtoById(id)
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
        log.trace(
                "getNetworksByAccount: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId);
        List<AccountNetworkDto> dtos = accountNetworkService.getByAccountId(accountId);
        log.trace("getNetworksByAccount: returning {} relationships", dtos.size());
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
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public List<AccountNetworkDto> getAccountsByNetwork(@PathVariable Long networkId) {
        log.trace(
                "getAccountsByNetwork: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        List<AccountNetworkDto> dtos = accountNetworkService.getByNetworkId(networkId);
        log.trace("getAccountsByNetwork: returning {} relationships", dtos.size());
        return dtos;
    }

    /**
     * GET /api/account-networks/networks-by-account/5
     *
     * <p>Get network entities accessible by an account (direct network objects)
     */
    @GetMapping("/networks-by-account/{accountId}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkDto> getNetworkEntitiesByAccount(@PathVariable Long accountId) {
        log.trace(
                "getNetworkEntitiesByAccount: apiUser={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId);
        var networks = accountNetworkService.getNetworkDtosByAccountId(accountId);
        log.trace("getNetworkEntitiesByAccount: returning {} networks", networks.size());
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
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public List<AccountDto> getAccountEntitiesByNetwork(@PathVariable Long networkId) {
        log.trace(
                "getAccountEntitiesByNetwork: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        var accounts = accountNetworkService.getAccountDtosByNetworkId(networkId);
        log.trace("getAccountEntitiesByNetwork: returning {} accounts", accounts.size());
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
        log.trace(
                "checkAccess: apiUser={}, accountId={}, networkId={}",
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
    public ResponseEntity<AccountNetworkDto> createAccountNetwork(
            @RequestBody SaveAccountNetworkRequest request) {
        log.trace(
                "createAccountNetwork: apiUser={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                request.accountId(),
                request.networkId());
        AccountNetworkDto saved = accountNetworkService.saveAndReturnDto(request, null);
        log.trace("createAccountNetwork: created relationship with id={}", saved.id());
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
    public ResponseEntity<AccountNetworkDto> grantAccess(
            @RequestParam Long accountId, @RequestParam Long networkId) {
        log.trace(
                "grantAccess: apiUser={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);
        try {
            AccountNetworkDto saved =
                    accountNetworkService.grantAccessAndReturnDto(accountId, networkId);
            log.trace("grantAccess: created relationship with id={}", saved.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            log.trace("grantAccess: failed - {}", e.getMessage());
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
    public ResponseEntity<AccountNetworkDto> updateAccountNetwork(
            @PathVariable Long id, @RequestBody SaveAccountNetworkRequest request) {
        log.trace(
                "updateAccountNetwork: apiUser={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        // Verify relationship exists
        if (!accountNetworkService.findDtoById(id).isPresent()) {
            log.trace("updateAccountNetwork: relationship not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        AccountNetworkDto updated = accountNetworkService.saveAndReturnDto(request, id);
        log.trace("updateAccountNetwork: updated relationship with id={}", updated.id());
        return ResponseEntity.ok(updated);
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
        log.trace(
                "deleteAccountNetwork: apiUser={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        if (!accountNetworkService.findDtoById(id).isPresent()) {
            log.trace("deleteAccountNetwork: relationship not found, id={}", id);
            return ResponseEntity.notFound().build();
        }

        accountNetworkService.delete(id);
        log.trace("deleteAccountNetwork: deleted relationship with id={}", id);
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
        log.trace(
                "revokeAccess: apiUser={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);
        accountNetworkService.revokeAccess(accountId, networkId);
        log.trace(
                "revokeAccess: revoked access for accountId={}, networkId={}",
                accountId,
                networkId);
        return ResponseEntity.noContent().build();
    }
}
