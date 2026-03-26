package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.AlertDto;
import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.entity.AlertEntity;
import com.matjazt.netmon2.mapper.AlertMapper;
import com.matjazt.netmon2.repository.AlertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Service for reading Alert data. */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final NetworkService networkService;
    private final DeviceService deviceService;

    public Optional<AlertDto> findById(Long id) {
        log.trace(
                "findById: apiUser={}, alertId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return alertRepository
                .findById(id)
                .map(
                        entity ->
                                alertMapper.toDto(
                                        entity,
                                        networkService.getAllNetworksAsMap(),
                                        buildDeviceMap(List.of(entity))));
    }

    private List<AlertDto> convertAndSortByTimeDesc(List<AlertEntity> entities) {
        var networkMap = networkService.getAllNetworksAsMap();
        var deviceMap = buildDeviceMap(entities);
        return alertMapper.toDtos(entities, networkMap, deviceMap).stream()
                .sorted((o1, o2) -> o2.timestamp().compareTo(o1.timestamp()))
                .toList();
    }

    public List<AlertDto> findByNetworkId(Long networkId) {
        log.trace(
                "findByNetworkId: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        return convertAndSortByTimeDesc(alertRepository.findByNetwork_Id(networkId));
    }

    public List<AlertDto> findActiveByNetworkId(Long networkId) {
        log.trace(
                "findActiveByNetworkId: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        return convertAndSortByTimeDesc(
                alertRepository.findByNetwork_IdAndClosureTimestampIsNull(networkId));
    }

    public List<AlertDto> findByDeviceId(Long deviceId) {
        log.trace(
                "findByDeviceId: apiUser={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return convertAndSortByTimeDesc(alertRepository.findByDevice_Id(deviceId));
    }

    public List<AlertDto> findActiveByDeviceId(Long deviceId) {
        log.trace(
                "findActiveByDeviceId: apiUser={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return convertAndSortByTimeDesc(
                alertRepository.findByDevice_IdAndClosureTimestampIsNull(deviceId));
    }

    // ========== PRIVATE HELPERS ==========

    private Map<Long, DeviceDto> buildDeviceMap(Collection<AlertEntity> entities) {
        Map<Long, DeviceDto> result = new HashMap<>();
        entities.stream()
                .map(e -> e.getNetwork() != null ? e.getNetwork().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(nid -> result.putAll(deviceService.getNetworkDevicesAsMap(nid)));
        return result;
    }
}
