package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.DeviceStatusHistoryDto;
import com.matjazt.netmon2.dto.response.DeviceStatusHistoryResponseDto;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;
import com.matjazt.netmon2.mapper.DeviceStatusHistoryApiMapper;
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
 * <p>@RestController combines @Controller and @ResponseBody
 *
 * <p>Provides read-only endpoints for device status history with pagination and filtering.
 */
@RestController
@RequestMapping("/api/device-status-history")
@PreAuthorize("hasAnyRole('admin', 'user')")
@Slf4j
@RequiredArgsConstructor
public class DeviceStatusHistoryController {

    private final DeviceStatusHistoryService deviceStatusHistoryService;
    private final DeviceStatusHistoryApiMapper deviceStatusHistoryApiMapper;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /**
     * GET /api/device-status-history/paginated?page=0&size=50
     *
     * <p>Get all device status history with pagination
     *
     * <p>Sorted by timestamp descending (newest first)
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<DeviceStatusHistoryResponseDto> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getAllPaginated: user={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getAllPaginated(page, size);
        var respPage = deviceStatusHistoryApiMapper.toResponsePage(dtoPage);
        logger.trace("getAllPaginated: returning {} history records", respPage.getSize());
        return respPage;
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
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public ResponseEntity<DeviceStatusHistoryEntity> getById(@PathVariable Long id) {
        logger.trace(
                "getById: user={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return deviceStatusHistoryService
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/device-status-history/device/5?page=0&size=50
     *
     * <p>Get device status history by device with pagination
     */
    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Page<DeviceStatusHistoryResponseDto> getByDevice(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getByDevice: user={}, deviceId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByDevice(deviceId, page, size);
        var respPage = deviceStatusHistoryApiMapper.toResponsePage(dtoPage);
        logger.trace("getByDevice: returning {} history records", respPage.getSize());
        return respPage;
    }

    /**
     * GET /api/device-status-history/network/5?page=0&size=50
     *
     * <p>Get device status history by network with pagination
     */
    @GetMapping("/network/{networkId}")
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public Page<DeviceStatusHistoryResponseDto> getByNetwork(
            @PathVariable Long networkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getByNetwork: user={}, networkId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByNetwork(networkId, page, size);
        var respPage = deviceStatusHistoryApiMapper.toResponsePage(dtoPage);
        logger.trace("getByNetwork: returning {} history records", respPage.getSize());
        return respPage;
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
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<DeviceStatusHistoryResponseDto> getByTimestampRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getByTimestampRange: user={}, minTimestamp={}, maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Page<DeviceStatusHistoryDto> dtoPage =
                deviceStatusHistoryService.getByTimestampRange(
                        minTimestamp, maxTimestamp, page, size);
        var respPage = deviceStatusHistoryApiMapper.toResponsePage(dtoPage);
        logger.trace("getByTimestampRange: returning {} history records", respPage.getSize());
        return respPage;
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
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Page<DeviceStatusHistoryResponseDto> getByDeviceAndTimestampRange(
            @PathVariable Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getByDeviceAndTimestampRange: user={}, deviceId={}, minTimestamp={},"
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
        var respPage = deviceStatusHistoryApiMapper.toResponsePage(dtoPage);
        logger.trace(
                "getByDeviceAndTimestampRange: returning {} history records", respPage.getSize());
        return respPage;
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
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public Page<DeviceStatusHistoryResponseDto> getByNetworkAndTimestampRange(
            @PathVariable Long networkId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getByNetworkAndTimestampRange: user={}, networkId={}, minTimestamp={},"
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
        var respPage = deviceStatusHistoryApiMapper.toResponsePage(dtoPage);
        logger.trace(
                "getByNetworkAndTimestampRange: returning {} history records", respPage.getSize());
        return respPage;
    }

    /**
     * GET /api/device-status-history/device/5/count
     *
     * <p>Count status changes for a device
     */
    @GetMapping("/device/{deviceId}/count")
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public long countByDevice(@PathVariable Long deviceId) {
        logger.trace(
                "countByDevice: user={}, deviceId={}",
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
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public long countByTimestampRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        logger.trace(
                "countByTimestampRange: user={}, start={}, end={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                start,
                end);
        return deviceStatusHistoryService.countByTimestampRange(start, end);
    }
}
