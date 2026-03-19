package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.LogDto;
import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.entity.LogEntity;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting LogEntity to LogDto.
 *
 * <p>MapStruct generates the Spring bean. All methods are manual defaults because the DTO includes
 * resolved display names (networkName, deviceNameOrVendor) that require lookup maps obtained from
 * the cache layer — these cannot be derived from the entity alone.
 */
@Mapper(componentModel = "spring")
public interface LogMapper {

    default LogDto toDto(
            LogEntity entity, Map<Long, NetworkDto> networkMap, Map<Long, DeviceDto> deviceMap) {
        Long networkId = entity.getNetwork() != null ? entity.getNetwork().getId() : null;
        Long deviceId = entity.getDevice() != null ? entity.getDevice().getId() : null;
        NetworkDto network = networkId != null ? networkMap.get(networkId) : null;
        DeviceDto device = deviceId != null ? deviceMap.get(deviceId) : null;
        return new LogDto(
                entity.getId(),
                entity.getTimestamp().toInstant(ZoneOffset.UTC),
                entity.getLevel(),
                entity.getOrigin(),
                entity.getMessage(),
                networkId,
                deviceId,
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

    default List<LogDto> toDtos(
            List<LogEntity> entities,
            Map<Long, NetworkDto> networkMap,
            Map<Long, DeviceDto> deviceMap) {
        return entities.stream().map(e -> toDto(e, networkMap, deviceMap)).toList();
    }

    default Page<LogDto> toDtoPage(
            Page<LogEntity> page,
            Map<Long, NetworkDto> networkMap,
            Map<Long, DeviceDto> deviceMap) {
        return page.map(e -> toDto(e, networkMap, deviceMap));
    }
}
