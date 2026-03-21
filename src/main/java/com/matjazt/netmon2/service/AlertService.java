package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.AlertDto;
import com.matjazt.netmon2.entity.AlertEntity;
import com.matjazt.netmon2.mapper.AlertMapper;
import com.matjazt.netmon2.repository.AlertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Service for reading Alert data. */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AlertDto> findById(Long id) {
        log.trace(
                "findById: apiUser={}, alertId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return alertRepository.findById(id).map(alertMapper::toDto);
    }

    private List<AlertDto> convertAndSortByTimeDesc(List<AlertEntity> entities) {

        return alertMapper.toDtos(entities).stream()
                .sorted((o1, o2) -> o2.timestamp().compareTo(o1.timestamp()))
                .toList();
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findByNetworkId(Long networkId) {
        log.trace(
                "findByNetworkId: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        return convertAndSortByTimeDesc(alertRepository.findByNetwork_Id(networkId));
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findActiveByNetworkId(Long networkId) {
        log.trace(
                "findActiveByNetworkId: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        return convertAndSortByTimeDesc(
                alertRepository.findByNetwork_IdAndClosureTimestampIsNull(networkId));
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findByDeviceId(Long deviceId) {
        log.trace(
                "findByDeviceId: apiUser={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return convertAndSortByTimeDesc(alertRepository.findByDevice_Id(deviceId));
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findActiveByDeviceId(Long deviceId) {
        log.trace(
                "findActiveByDeviceId: apiUser={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return convertAndSortByTimeDesc(
                alertRepository.findByDevice_IdAndClosureTimestampIsNull(deviceId));
    }
}
