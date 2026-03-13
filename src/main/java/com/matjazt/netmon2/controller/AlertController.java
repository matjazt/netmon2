package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.AlertDto;
import com.matjazt.netmon2.service.AlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for reading Alert data.
 *
 * <p>All endpoints are read-only. Alert lifecycle is managed internally by the system.
 */
@RestController
@RequestMapping("/api/alerts")
@PreAuthorize("hasAnyRole('admin', 'system')")
@Slf4j
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /** GET /api/alerts/{id} — get a single alert by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<AlertDto> getAlertById(@PathVariable Long id) {
        logger.trace(
                "getAlertById: user={}, alertId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return alertService
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/alerts/network/{networkId}?active=false — get alerts for a network.
     *
     * <p>When {@code active=true} (default false), returns only open alerts.
     */
    @GetMapping("/network/{networkId}")
    public List<AlertDto> getAlertsByNetwork(
            @PathVariable Long networkId, @RequestParam(defaultValue = "false") boolean active) {
        logger.trace(
                "getAlertsByNetwork: user={}, networkId={}, active={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                active);
        return active
                ? alertService.findActiveByNetworkId(networkId)
                : alertService.findByNetworkId(networkId);
    }

    /**
     * GET /api/alerts/device/{deviceId}?active=false — get alerts for a device.
     *
     * <p>When {@code active=true} (default false), returns only open alerts.
     */
    @GetMapping("/device/{deviceId}")
    public List<AlertDto> getAlertsByDevice(
            @PathVariable Long deviceId, @RequestParam(defaultValue = "false") boolean active) {
        logger.trace(
                "getAlertsByDevice: user={}, deviceId={}, active={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                active);
        return active
                ? alertService.findActiveByDeviceId(deviceId)
                : alertService.findByDeviceId(deviceId);
    }
}
