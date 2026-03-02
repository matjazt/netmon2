package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.DeviceStatusHistoryDto;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between DeviceStatusHistoryEntity and DeviceStatusHistoryDto.
 *
 * <p>@Mapper(componentModel = "spring") makes this a Spring bean that can be injected.
 *
 * <p>MapStruct generates the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface DeviceStatusHistoryMapper {

    /**
     * Convert DeviceStatusHistoryEntity to DeviceStatusHistoryDto.
     *
     * <p>@Mapping maps nested network.id and device.id to flat fields.
     */
    @Mapping(source = "network.id", target = "networkId")
    @Mapping(source = "device.id", target = "deviceId")
    DeviceStatusHistoryDto toDto(DeviceStatusHistoryEntity entity);

    /**
     * Convert list of DeviceStatusHistoryEntity to list of DeviceStatusHistoryDto.
     *
     * <p>MapStruct automatically generates this using toDto() for each element.
     */
    List<DeviceStatusHistoryDto> toDtos(List<DeviceStatusHistoryEntity> entities);

    /**
     * Convert Page of DeviceStatusHistoryEntity to Page of DeviceStatusHistoryDto.
     *
     * <p>Default method allows custom logic for Page mapping.
     */
    default Page<DeviceStatusHistoryDto> toDtoPage(Page<DeviceStatusHistoryEntity> page) {
        return page.map(this::toDto);
    }
}
