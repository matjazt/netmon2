package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.request.SaveNetworkRequest;
import com.matjazt.netmon2.service.NetworkService;

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
 * REST Controller for managing Network entities.
 *
 * <p>Provides CRUD endpoints for monitored networks.
 */
@RestController
@RequestMapping("/api/networks")
@PreAuthorize("hasAnyRole('admin')")
@Slf4j
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /** GET /api/networks — get all networks. */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin')")
    public List<NetworkDto> getAllNetworks() {
        log.trace(
                "getAllNetworks: apiUser={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<NetworkDto> dtos = networkService.findAllNetworkSummaries();
        log.trace("getAllNetworks: returning {} networks", dtos.size());
        return dtos;
    }

    /**
     * GET /api/networks/5
     *
     * <p>Get network by ID.
     *
     * <p>Returns:
     *
     * <ul>
     *   <li>200 OK with network JSON if found
     *   <li>404 Not Found if network doesn't exist
     * </ul>
     */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canReadNetwork(authentication,"
                    + " #id)")
    public ResponseEntity<NetworkDto> getNetworkById(@PathVariable Long id) {
        log.trace(
                "getNetworkById: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return networkService
                .findNetworkDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/networks/exists?name=HomeNetwork
     *
     * <p>Check if network exists (returns boolean)
     */
    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('admin')")
    public boolean checkNetworkExists(@RequestParam String name) {
        log.trace(
                "checkNetworkExists: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                name);
        return networkService.networkExistsByName(name);
    }

    // ========== POST ENDPOINTS (create new resources) ==========

    /**
     * POST /api/networks
     *
     * <p>Create a new network.
     *
     * <p>Returns 201 Created with the saved network.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin')")
    public ResponseEntity<NetworkDto> createNetwork(@RequestBody SaveNetworkRequest request) {
        log.trace(
                "createNetwork: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                request.name());
        NetworkDto saved = networkService.createNetworkAndReturnDto(request);
        log.trace("createNetwork: created network with id={}", saved.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ========== PUT ENDPOINTS (update existing resources) ==========

    /**
     * PUT /api/networks/5
     *
     * <p>Update an existing network.
     */
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canWriteNetwork(authentication,"
                    + " #id)")
    public ResponseEntity<NetworkDto> updateNetwork(
            @PathVariable Long id, @RequestBody SaveNetworkRequest request) {
        log.trace(
                "updateNetwork: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        NetworkDto updated = networkService.updateNetworkAndReturnDto(request, id);
        log.trace("updateNetwork: updated network with id={}", updated.id());
        return ResponseEntity.ok(updated);
    }

    // ========== DELETE ENDPOINTS (remove resources) ==========

    /**
     * DELETE /api/networks/5
     *
     * <p>Delete a network
     *
     * <p>Returns 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin')")
    public ResponseEntity<Void> deleteNetwork(@PathVariable Long id) {
        log.trace(
                "deleteNetwork: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);

        networkService.deleteNetwork(id);
        log.trace("deleteNetwork: deleted network with id={}", id);
        return ResponseEntity.noContent().build();
    }
}
