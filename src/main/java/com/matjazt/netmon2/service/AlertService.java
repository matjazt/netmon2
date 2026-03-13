package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.AlertDto;
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
        logger.trace(
                "findById: user={}, alertId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return alertRepository.findById(id).map(alertMapper::toDto);
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findByNetworkId(Long networkId) {
        logger.trace(
                "findByNetworkId: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        return alertMapper.toDtos(alertRepository.findByNetwork_Id(networkId));
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findActiveByNetworkId(Long networkId) {
        logger.trace(
                "findActiveByNetworkId: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        return alertMapper.toDtos(
                alertRepository.findByNetwork_IdAndClosureTimestampIsNull(networkId));
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findByDeviceId(Long deviceId) {
        logger.trace(
                "findByDeviceId: user={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return alertMapper.toDtos(alertRepository.findByDevice_Id(deviceId));
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AlertDto> findActiveByDeviceId(Long deviceId) {
        logger.trace(
                "findActiveByDeviceId: user={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return alertMapper.toDtos(
                alertRepository.findByDevice_IdAndClosureTimestampIsNull(deviceId));
    }
}
