package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.LogDto;
import com.matjazt.netmon2.service.LogService;

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
 * REST Controller for reading Log entities.
 *
 * <p>@RestController combines @Controller and @ResponseBody
 *
 * <p>Provides read-only endpoints for log entries with pagination and filtering.
 */
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasAnyRole('admin', 'user')")
@Slf4j
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    // ========== GET ENDPOINTS (retrieve data) ==========

    /**
     * GET /api/logs/paginated?page=0&size=50
     *
     * <p>Get all logs with pagination
     *
     * <p>Sorted by timestamp descending (newest first)
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<LogDto> getAllLogsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getAllLogsPaginated: user={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Page<LogDto> dtoPage = logService.getAllLogsPaginated(page, size);
        logger.trace("getAllLogsPaginated: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET /api/logs/5
     *
     * <p>Get log by ID
     *
     * <p>Returns:
     *
     * <ul>
     *   <li>200 OK with log JSON if found
     *   <li>404 Not Found if log doesn't exist
     * </ul>
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public ResponseEntity<LogDto> getLogById(@PathVariable Long id) {
        logger.trace(
                "getLogById: user={}, logId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return logService
                .findLogDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/logs/network/5?page=0&size=50
     *
     * <p>Get logs by network with pagination
     */
    @GetMapping("/network/{networkId}")
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public Page<LogDto> getLogsByNetwork(
            @PathVariable Long networkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getLogsByNetwork: user={}, networkId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                page,
                size);
        Page<LogDto> dtoPage = logService.getLogsByNetwork(networkId, page, size);
        logger.trace("getLogsByNetwork: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET /api/logs/device/5?page=0&size=50
     *
     * <p>Get logs by device with pagination
     */
    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Page<LogDto> getLogsByDevice(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getLogsByDevice: user={}, deviceId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                page,
                size);
        Page<LogDto> dtoPage = logService.getLogsByDevice(deviceId, page, size);
        logger.trace("getLogsByDevice: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET
     * /api/logs/by-timestamp?minTimestamp=2025-01-01T00:00:00&maxTimestamp=2025-12-31T23:59:59&page=0&size=50
     *
     * <p>Get logs within a timestamp range with pagination
     *
     * <p>Timestamp format: ISO-8601 (e.g., 2025-01-01T00:00:00)
     */
    @GetMapping("/by-timestamp")
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<LogDto> getLogsByTimestampRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getLogsByTimestampRange: user={}, minTimestamp={}, maxTimestamp={}, page={},"
                        + " size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Page<LogDto> dtoPage =
                logService.getLogsByTimestampRange(minTimestamp, maxTimestamp, page, size);
        logger.trace("getLogsByTimestampRange: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET
     * /api/logs/network/5/by-timestamp?minTimestamp=2025-01-01T00:00:00&maxTimestamp=2025-12-31T23:59:59&page=0&size=50
     *
     * <p>Get logs by network and timestamp range with pagination
     *
     * <p>Timestamp format: ISO-8601 (e.g., 2025-01-01T00:00:00)
     */
    @GetMapping("/network/{networkId}/by-timestamp")
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public Page<LogDto> getLogsByNetworkAndTimestampRange(
            @PathVariable Long networkId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getLogsByNetworkAndTimestampRange: user={}, networkId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Page<LogDto> dtoPage =
                logService.getLogsByNetworkAndTimestampRange(
                        networkId, minTimestamp, maxTimestamp, page, size);
        logger.trace("getLogsByNetworkAndTimestampRange: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * GET
     * /api/logs/device/5/by-timestamp?minTimestamp=2025-01-01T00:00:00&maxTimestamp=2025-12-31T23:59:59&page=0&size=50
     *
     * <p>Get logs by device and timestamp range with pagination
     *
     * <p>Timestamp format: ISO-8601 (e.g., 2025-01-01T00:00:00)
     */
    @GetMapping("/device/{deviceId}/by-timestamp")
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Page<LogDto> getLogsByDeviceAndTimestampRange(
            @PathVariable Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime minTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime maxTimestamp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        logger.trace(
                "getLogsByDeviceAndTimestampRange: user={}, deviceId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Page<LogDto> dtoPage =
                logService.getLogsByDeviceAndTimestampRange(
                        deviceId, minTimestamp, maxTimestamp, page, size);
        logger.trace("getLogsByDeviceAndTimestampRange: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }
}
