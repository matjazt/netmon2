package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.request.SaveDeviceRequest;
import com.matjazt.netmon2.entity.DeviceOperationMode;
import com.matjazt.netmon2.service.DeviceService;

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
 * REST Controller for managing Device entities.
 *
 * <p>Provides CRUD endpoints for devices.
 */
@RestController
@RequestMapping("/api/devices")
@PreAuthorize("hasAnyRole('admin')")
@Slf4j
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /** GET /api/devices — get all devices. */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin')")
    public List<DeviceDto> getAllDevices() {
        return deviceService.findAllDeviceSummaries();
    }

    /**
     * GET /api/devices/{id} — get device by ID.
     *
     * <p>Returns 404 if not found.
     */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or"
                    + " @networkAuthorizationService.canAccessDevice(authentication, #id)")
    public ResponseEntity<DeviceDto> getDeviceById(@PathVariable Long id) {
        return deviceService
                .findDeviceDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/devices/network/{networkId} — get all devices on a network. */
    @GetMapping("/network/{networkId}")
    @PreAuthorize(
            "hasAnyRole('admin') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public List<DeviceDto> getDevicesByNetwork(@PathVariable Long networkId) {
        log.trace(
                "getDevicesByNetwork: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        List<DeviceDto> dtos = deviceService.getDevicesByNetwork(networkId);
        log.trace("getDevicesByNetwork: returning {} devices", dtos.size());
        return dtos;
    }

    /** GET /api/devices/network/{networkId}/stats — get device statistics for a network. */
    @GetMapping("/network/{networkId}/stats")
    @PreAuthorize(
            "hasAnyRole('admin') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public DeviceService.DeviceStats getDeviceStats(@PathVariable Long networkId) {
        return deviceService.getDeviceStats(networkId);
    }

    /** GET /api/devices/exists?networkId=&macAddress= — check if device exists by MAC address. */
    @GetMapping("/exists")
    @PreAuthorize(
            "hasAnyRole('admin') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public boolean checkDeviceExists(
            @RequestParam Long networkId, @RequestParam String macAddress) {
        return deviceService.deviceExists(networkId, macAddress);
    }

    // ========== POST ENDPOINTS (create new resources) ==========

    /**
     * POST /api/devices — create a new device.
     *
     * <p>Returns 201 Created with the saved device.
     */
    @PostMapping
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canAccessNetwork(authentication,"
                    + " #request.networkId)")
    public ResponseEntity<DeviceDto> createDevice(@RequestBody SaveDeviceRequest request) {
        DeviceDto saved = deviceService.saveDeviceAndReturnDto(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ========== PUT ENDPOINTS (update existing resources) ==========

    /** PUT /api/devices/{id} — update an existing device. */
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canAccessNetwork(authentication,"
                    + " #request.networkId)")
    public ResponseEntity<DeviceDto> updateDevice(
            @PathVariable Long id, @RequestBody SaveDeviceRequest request) {

        DeviceDto updated = deviceService.saveDeviceAndReturnDto(request, id);
        return ResponseEntity.ok(updated);
    }

    /** PUT /api/devices/{id}/mode?mode= — update the device operation mode. */
    @PutMapping("/{id}/mode")
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canAccessDevice(authentication,"
                    + " #id)")
    public ResponseEntity<DeviceDto> updateDeviceMode(
            @PathVariable Long id, @RequestParam DeviceOperationMode mode) {
        try {
            DeviceDto updated = deviceService.updateDeviceModeAndReturnDto(id, mode);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** PUT /api/devices/{id}/name?name= — update the device name. */
    @PutMapping("/{id}/name")
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canAccessDevice(authentication,"
                    + " #id)")
    public ResponseEntity<DeviceDto> updateDeviceName(
            @PathVariable Long id, @RequestParam String name) {
        try {
            DeviceDto updated = deviceService.renameDeviceAndReturnDto(id, name);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== DELETE ENDPOINTS (remove resources) ==========

    /**
     * DELETE /api/devices/{id} — delete a device.
     *
     * <p>Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canAccessDevice(authentication,"
                    + " #id)")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        if (deviceService.findDeviceDtoById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
