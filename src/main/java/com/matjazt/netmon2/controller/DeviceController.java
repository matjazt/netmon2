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
 * REST Controller demonstrating how to use services and repositories.
 *
 * <p>@RestController combines @Controller and @ResponseBody
 *
 * <ul>
 *   <li>Makes this a REST API controller
 *   <li>Automatically serializes return values to JSON
 * </ul>
 *
 * <p>@RequestMapping sets the base URL path for all endpoints
 *
 * <p>SPRING MVC REQUEST MAPPINGS:
 *
 * <ul>
 *   <li>@GetMapping - HTTP GET (retrieve data)
 *   <li>@PostMapping - HTTP POST (create new resource)
 *   <li>@PutMapping - HTTP PUT (update existing resource)
 *   <li>@DeleteMapping - HTTP DELETE (remove resource)
 * </ul>
 *
 * <p>PATH VARIABLES vs QUERY PARAMETERS:
 *
 * <ul>
 *   <li>@PathVariable: /devices/{id} - required, part of URL path
 *   <li>@RequestParam: /devices?name=value - optional or multiple values
 * </ul>
 *
 * <p>RESPONSE ENTITY:
 *
 * <ul>
 *   <li>Allows controlling HTTP status codes
 *   <li>ResponseEntity.ok() = 200 OK
 *   <li>ResponseEntity.notFound() = 404 Not Found
 *   <li>ResponseEntity.status(HttpStatus.CREATED) = 201 Created
 * </ul>
 */
@RestController
@RequestMapping("/api/devices")
@PreAuthorize("hasAnyRole('admin')")
@Slf4j
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /**
     * EXAMPLE: GET /api/devices
     *
     * <p>Get all devices (careful with large datasets!) Returns 200 OK with JSON array of devices
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin')")
    public List<DeviceDto> getAllDevices() {
        return deviceService.findAllDeviceSummaries();
    }

    /**
     * EXAMPLE: GET /api/devices/5
     *
     * <p>Get device by ID
     *
     * <p>@PathVariable extracts {id} from URL path
     *
     * <p>Returns:
     *
     * <ul>
     *   <li>200 OK with device JSON if found
     *   <li>404 Not Found if device doesn't exist
     * </ul>
     */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or"
                    + " @deviceAuthorizationService.canAccessDevice(authentication, #id)")
    public ResponseEntity<DeviceDto> getDeviceById(@PathVariable Long id) {
        return deviceService
                .findDeviceDtoById(id)
                .map(ResponseEntity::ok) // If found, return 200 OK
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404
    }

    /**
     * EXAMPLE: GET /api/devices/network/5
     *
     * <p>Get all devices on a specific network
     */
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

    /**
     * EXAMPLE: GET /api/devices/network/5/stats
     *
     * <p>Get device statistics for a network
     *
     * <p>Returns custom object (not entity) as JSON
     */
    @GetMapping("/network/{networkId}/stats")
    @PreAuthorize(
            "hasAnyRole('admin') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public DeviceService.DeviceStats getDeviceStats(@PathVariable Long networkId) {
        return deviceService.getDeviceStats(networkId);
    }

    /**
     * EXAMPLE: GET /api/devices/exists?networkId=5&macAddress=AA:BB:CC:DD:EE:FF
     *
     * <p>Check if device exists (returns boolean)
     */
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
     * EXAMPLE: POST /api/devices
     *
     * <p>Create a new device
     *
     * <p>@RequestBody deserializes JSON from request body to DeviceEntity
     *
     * <p>Request body example:
     *
     * <pre>{@code
     * {
     *   "networkId": 5,
     *   "macAddress": "AA:BB:CC:DD:EE:FF",
     *   "ipAddress": "192.168.1.100",
     *   "online": true,
     *   "name": "My Device"
     * }
     * }</pre>
     *
     * <p>Returns 201 Created with the saved device (including generated ID)
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

    /**
     * EXAMPLE: PUT /api/devices/5
     *
     * <p>Update an existing device
     *
     * <p>ID in path + full entity in body
     */
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or @networkAuthorizationService.canAccessNetwork(authentication,"
                    + " #request.networkId)")
    public ResponseEntity<DeviceDto> updateDevice(
            @PathVariable Long id, @RequestBody SaveDeviceRequest request) {

        DeviceDto updated = deviceService.saveDeviceAndReturnDto(request, id);
        return ResponseEntity.ok(updated);
    }

    /**
     * EXAMPLE: PUT /api/devices/5/mode?mode=ALWAYS_ON
     *
     * <p>Update only the operation mode
     *
     * <p>Partial update - only changes one field
     */
    @PutMapping("/{id}/mode")
    @PreAuthorize(
            "hasAnyRole('admin') or @deviceAuthorizationService.canAccessDevice(authentication,"
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

    /**
     * EXAMPLE: PUT /api/devices/5/name?name=NewDeviceName
     *
     * <p>Update only the device name
     *
     * <p>Partial update - only changes one field
     */
    @PutMapping("/{id}/name")
    @PreAuthorize(
            "hasAnyRole('admin') or @deviceAuthorizationService.canAccessDevice(authentication,"
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
     * EXAMPLE: DELETE /api/devices/5
     *
     * <p>Delete a device
     *
     * <p>Returns 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('admin') or @deviceAuthorizationService.canAccessDevice(authentication,"
                    + " #id)")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        if (deviceService.findDeviceDtoById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
