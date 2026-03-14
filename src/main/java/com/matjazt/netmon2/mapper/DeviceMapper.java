package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.request.SaveDeviceRequest;
import com.matjazt.netmon2.entity.DeviceEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between DeviceEntity and DeviceDto.
 *
 * <p>@Mapper(componentModel = "spring") makes this a Spring bean that can be injected.
 *
 * <p>MapStruct generates the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface DeviceMapper {

    /**
     * Convert DeviceEntity to DeviceDto.
     *
     * <p>@Mapping maps nested network.id to flat networkId field.
     */
    @Mapping(source = "network.id", target = "networkId")
    DeviceDto toDto(DeviceEntity entity);

    @Mapping(source = "networkId", target = "network.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deviceOperationModeRef", ignore = true)
    DeviceEntity toEntity(SaveDeviceRequest request);

    /**
     * Convert list of DeviceEntity to list of DeviceDto.
     *
     * <p>MapStruct automatically generates this using toDto() for each element.
     */
    List<DeviceDto> toDtos(List<DeviceEntity> entities);
}
