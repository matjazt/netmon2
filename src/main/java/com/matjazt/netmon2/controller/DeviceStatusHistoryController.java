package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.DeviceStatusHistoryDto;
import com.matjazt.netmon2.service.DeviceStatusHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST Controller for reading DeviceStatusHistory entities.
 *
 * <p>Provides read-only endpoints for device status history with pagination and filtering.
 */
@RestController
@RequestMapping("/api/device-status-history")
@PreAuthorize("hasAnyRole('admin')")
@Slf4j
@RequiredArgsConstructor
public class DeviceStatusHistoryController {

    private final DeviceStatusHistoryService deviceStatusHistoryService;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /**
     * GET /api/device-status-history/paginated?page=0&size=50
     *
     * <p>Get all device status history with pagination
     *
     * <p>Sorted by timestamp descending (newest first)
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('admin')")
    public Page<DeviceStatusHistoryDto> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.trace(
                "getAllPaginated: apiUser={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getAllPaginated(page, size);
        log.trace("getAllPaginated: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET /api/device-status-history/5
     *
     * <p>Get device status history by ID
     *
     * <p>Returns:
     *
     * <ul>
     *   <li>200 OK with history JSON if found
     *   <li>404 Not Found if history doesn't exist
     * </ul>
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin')")
    public ResponseEntity<DeviceStatusHistoryDto> getById(@PathVariable Long id) {
        log.trace(
                "getById: apiUser={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return deviceStatusHistoryService
                .findDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/device-status-history/device/5?page=0&size=50
     *
     * <p>Get device status history by device with pagination
     */
    @GetMapping("/device/{deviceId}")
    @PreAuthorize(
            "hasAnyRole('admin') or @deviceAuthorizationService.canAccessDevice(authentication,"
                    + " #deviceId)")
    public Page<DeviceStatusHistoryDto> getByDevice(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.trace(
                "getByDevice: apiUser={}, deviceId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByDevice(deviceId, page, size);
        log.trace("getByDevice: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET /api/device-status-history/network/5?page=0&size=50
     *
     * <p>Get device status history by network with pagination
     */
    @GetMapping("/network/{networkId}")
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public Page<DeviceStatusHistoryDto> getByNetwork(
            @PathVariable Long networkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.trace(
                "getByNetwork: apiUser={}, networkId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByNetwork(networkId, page, size);
        log.trace("getByNetwork: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET
     * /api/device-status-history/by-timestamp?minTimestamp=2025-01-01T00:00:00&maxTimestamp=2025-12-31T23:59:59&page=0&size=50
     *
     * <p>Get device status history within a timestamp range with pagination
     *
     * <p>Timestamp format: ISO-8601 (e.g., 2025-01-01T00:00:00)
     */
    @GetMapping("/by-timestamp")
    @PreAuthorize("hasAnyRole('admin')")
    public Page<DeviceStatusHistoryDto> getByTimestampRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.trace(
                "getByTimestampRange: apiUser={}, minTimestamp={}, maxTimestamp={}, page={},"
                        + " size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByTimestampRange(
                        minTimestamp, maxTimestamp, page, size);
        log.trace("getByTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET
     * /api/device-status-history/device/5/by-timestamp?minTimestamp=2025-01-01T00:00:00&maxTimestamp=2025-12-31T23:59:59&page=0&size=50
     *
     * <p>Get device status history by device and timestamp range with pagination
     *
     * <p>Timestamp format: ISO-8601 (e.g., 2025-01-01T00:00:00)
     */
    @GetMapping("/device/{deviceId}/by-timestamp")
    @PreAuthorize(
            "hasAnyRole('admin') or @deviceAuthorizationService.canAccessDevice(authentication,"
                    + " #deviceId)")
    public Page<DeviceStatusHistoryDto> getByDeviceAndTimestampRange(
            @PathVariable Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.trace(
                "getByDeviceAndTimestampRange: apiUser={}, deviceId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByDeviceAndTimestampRange(
                        deviceId, minTimestamp, maxTimestamp, page, size);
        log.trace("getByDeviceAndTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET
     * /api/device-status-history/network/5/by-timestamp?minTimestamp=2025-01-01T00:00:00&maxTimestamp=2025-12-31T23:59:59&page=0&size=50
     *
     * <p>Get device status history by network and timestamp range with pagination
     *
     * <p>Timestamp format: ISO-8601 (e.g., 2025-01-01T00:00:00)
     */
    @GetMapping("/network/{networkId}/by-timestamp")
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public Page<DeviceStatusHistoryDto> getByNetworkAndTimestampRange(
            @PathVariable Long networkId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.trace(
                "getByNetworkAndTimestampRange: apiUser={}, networkId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByNetworkAndTimestampRange(
                        networkId, minTimestamp, maxTimestamp, page, size);
        log.trace("getByNetworkAndTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET /api/device-status-history/device/5/count
     *
     * <p>Count status changes for a device
     */
    @GetMapping("/device/{deviceId}/count")
    @PreAuthorize(
            "hasAnyRole('admin') or @deviceAuthorizationService.canAccessDevice(authentication,"
                    + " #deviceId)")
    public long countByDevice(@PathVariable Long deviceId) {
        log.trace(
                "countByDevice: apiUser={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return deviceStatusHistoryService.countByDevice(deviceId);
    }

    /**
     * GET
     * /api/device-status-history/count-by-timestamp?start=2025-01-01T00:00:00&end=2025-12-31T23:59:59
     *
     * <p>Count status changes in date range
     *
     * <p>Timestamp format: ISO-8601 (e.g., 2025-01-01T00:00:00)
     */
    @GetMapping("/count-by-timestamp")
    @PreAuthorize("hasAnyRole('admin')")
    public long countByTimestampRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.trace(
                "countByTimestampRange: apiUser={}, start={}, end={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                start,
                end);
        return deviceStatusHistoryService.countByTimestampRange(start, end);
    }
}
