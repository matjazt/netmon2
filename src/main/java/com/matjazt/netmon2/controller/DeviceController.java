package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.response.DeviceResponseDto;
import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.DeviceOperationMode;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;
import com.matjazt.netmon2.mapper.DeviceApiMapper;
import com.matjazt.netmon2.service.DeviceService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasAnyRole('admin', 'user')")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceApiMapper deviceApiMapper;

    /**
     * Constructor injection of service layer.
     *
     * <p>Service contains business logic and uses repositories.
     */
    public DeviceController(DeviceService deviceService, DeviceApiMapper deviceApiMapper) {
        this.deviceService = deviceService;
        this.deviceApiMapper = deviceApiMapper;
    }

    // ========== GET ENDPOINTS (retrieve data) ==========

    /**
     * EXAMPLE: GET /api/devices
     *
     * <p>Get all devices (careful with large datasets!) Returns 200 OK with JSON array of devices
     */
    @GetMapping
    public List<DeviceResponseDto> getAllDevices() {
        List<DeviceDto> dtos = deviceService.findAllDeviceSummaries();
        return deviceApiMapper.toResponses(dtos);
    }

    /**
     * EXAMPLE: GET /api/devices/paginated?page=0&size=20
     *
     * <p>Get devices with pagination
     *
     * <p>@RequestParam extracts query parameters from URL
     *
     * <p>defaultValue provides fallback if parameter is missing
     */
    @GetMapping("/paginated")
    public Page<DeviceResponseDto> getDevicesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DeviceDto> dtoPage = deviceService.getDeviceSummariesPaginated(null, page, size);
        return deviceApiMapper.toResponsePage(dtoPage);
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
    public ResponseEntity<DeviceEntity> getDeviceById(@PathVariable Long id) {
        return deviceService
                .findDeviceById(id)
                .map(ResponseEntity::ok) // If found, return 200 OK
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404
    }

    /**
     * EXAMPLE: GET /api/devices/mac/AA:BB:CC:DD:EE:FF
     *
     * <p>Find device by MAC address
     *
     * <p>MAC address is part of the URL path
     */
    @GetMapping("/mac/{macAddress}")
    public ResponseEntity<DeviceEntity> getDeviceByMac(@PathVariable String macAddress) {
        return deviceService
                .findDeviceByMac(macAddress)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * EXAMPLE: GET /api/devices/network/5
     *
     * <p>Get all devices on a specific network
     */
    @GetMapping("/network/{networkId}")
    public List<DeviceResponseDto> getDevicesByNetwork(@PathVariable Long networkId) {
        List<DeviceDto> dtos = deviceService.findDeviceSummariesByNetwork(networkId);
        return deviceApiMapper.toResponses(dtos);
    }

    /**
     * EXAMPLE: GET /api/devices/network/5/online
     *
     * <p>Get only online devices on a network
     */
    @GetMapping("/network/{networkId}/online")
    public List<DeviceResponseDto> getOnlineDevices(@PathVariable Long networkId) {
        List<DeviceDto> dtos = deviceService.findOnlineDeviceSummaries(networkId);
        return deviceApiMapper.toResponses(dtos);
    }

    /**
     * EXAMPLE: GET /api/devices/network/5/stats
     *
     * <p>Get device statistics for a network
     *
     * <p>Returns custom object (not entity) as JSON
     */
    @GetMapping("/network/{networkId}/stats")
    public DeviceService.DeviceStats getDeviceStats(@PathVariable Long networkId) {
        return deviceService.getDeviceStats(networkId);
    }

    /**
     * EXAMPLE: GET /api/devices/5/history?limit=50
     *
     * <p>Get device status history
     *
     * <p>Combines path variable and query parameter
     */
    @GetMapping("/{id}/history")
    public List<DeviceStatusHistoryEntity> getDeviceHistory(
            @PathVariable Long id, @RequestParam(defaultValue = "50") int limit) {
        return deviceService.getDeviceHistory(id, limit);
    }

    /**
     * EXAMPLE: GET /api/devices/needing-alerts
     *
     * <p>Get devices that need alert generation
     */
    @GetMapping("/needing-alerts")
    public List<DeviceEntity> getDevicesNeedingAlerts() {
        return deviceService.findDevicesNeedingAlerts();
    }

    /**
     * EXAMPLE: GET /api/devices/exists?networkId=5&macAddress=AA:BB:CC:DD:EE:FF
     *
     * <p>Check if device exists (returns boolean)
     */
    @GetMapping("/exists")
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
     *   "network": {"id": 5},
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
    public ResponseEntity<DeviceEntity> createDevice(@RequestBody DeviceEntity device) {
        DeviceEntity saved = deviceService.saveDevice(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * EXAMPLE: POST /api/devices/mqtt-update
     *
     * <p>Process MQTT device update Custom DTO (Data Transfer Object) for request body
     *
     * <p>Request body:
     *
     * <pre>{@code
     * {
     *   "networkId": 5,
     *   "macAddress": "AA:BB:CC:DD:EE:FF",
     *   "ipAddress": "192.168.1.100",
     *   "online": true
     * }
     * }</pre>
     */
    @PostMapping("/mqtt-update")
    public DeviceEntity processMqttUpdate(@RequestBody MqttDeviceUpdateRequest request) {
        return deviceService.processDeviceUpdate(
                request.networkId, request.macAddress, request.ipAddress, request.online);
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
    public ResponseEntity<DeviceEntity> updateDevice(
            @PathVariable Long id, @RequestBody DeviceEntity device) {

        // Verify device exists
        if (!deviceService.findDeviceById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Ensure ID matches
        device.setId(id);
        DeviceEntity updated = deviceService.saveDevice(device);
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
    public ResponseEntity<DeviceEntity> updateDeviceMode(
            @PathVariable Long id, @RequestParam DeviceOperationMode mode) {
        try {
            DeviceEntity updated = deviceService.updateDeviceMode(id, mode);
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
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        if (!deviceService.findDeviceById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ========== INNER CLASS (DTO) ==========

    /**
     * Data Transfer Object for MQTT update request.
     *
     * <p>In a real project, this would be in a separate dto package. DTOs separate API structure
     * from database entities.
     */
    public static class MqttDeviceUpdateRequest {
        public Long networkId;
        public String macAddress;
        public String ipAddress;
        public Boolean online;

        // Spring needs getters/setters or public fields for JSON deserialization
    }
}
