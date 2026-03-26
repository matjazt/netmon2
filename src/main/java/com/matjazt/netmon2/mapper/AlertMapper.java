package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.AlertDto;
import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.entity.AlertEntity;

import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting AlertEntity to AlertDto.
 *
 * <p>MapStruct generates the Spring bean. All methods are manual defaults because the DTO includes
 * resolved display names (networkName, deviceNameOrVendor) that require lookup maps obtained from
 * the cache layer — these cannot be derived from the entity alone.
 */
@Mapper(componentModel = "spring")
public interface AlertMapper {

    default AlertDto toDto(
            AlertEntity entity, Map<Long, NetworkDto> networkMap, Map<Long, DeviceDto> deviceMap) {
        Long networkId = entity.getNetwork() != null ? entity.getNetwork().getId() : null;
        Long deviceId = entity.getDevice() != null ? entity.getDevice().getId() : null;
        NetworkDto network = networkId != null ? networkMap.get(networkId) : null;
        DeviceDto device = deviceId != null ? deviceMap.get(deviceId) : null;
        return new AlertDto(
                entity.getId(),
                networkId,
                deviceId,
                entity.getAlertType(),
                entity.getMessage(),
                toInstant(entity.getTimestamp()),
                toInstant(entity.getClosureTimestamp()),
                network != null ? network.name() : null,
                resolveDeviceNameOrVendor(device));
    }

    default String resolveDeviceNameOrVendor(DeviceDto device) {
        if (device == null) return null;
        String name = device.name();
        if (name != null && !name.isBlank()) return name;
        String vendor = device.vendor();
        return vendor != null ? vendor : device.macAddress();
    }

    default List<AlertDto> toDtos(
            List<AlertEntity> entities,
            Map<Long, NetworkDto> networkMap,
            Map<Long, DeviceDto> deviceMap) {
        return entities.stream().map(e -> toDto(e, networkMap, deviceMap)).toList();
    }

    default Instant toInstant(LocalDateTime ldt) {
        return ldt != null ? ldt.toInstant(ZoneOffset.UTC) : null;
    }
}
