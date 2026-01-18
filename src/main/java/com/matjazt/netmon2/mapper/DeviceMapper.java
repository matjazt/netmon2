package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.entity.DeviceEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

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

    /**
     * Convert list of DeviceEntity to list of DeviceDto.
     *
     * <p>MapStruct automatically generates this using toDto() for each element.
     */
    List<DeviceDto> toDtos(List<DeviceEntity> entities);

    /**
     * Convert Page of DeviceEntity to Page of DeviceDto.
     *
     * <p>Default method allows custom logic for Page mapping.
     */
    default Page<DeviceDto> toDtoPage(Page<DeviceEntity> page) {
        return page.map(this::toDto);
    }
}
