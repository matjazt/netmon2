package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.LogDto;
import com.matjazt.netmon2.entity.LogEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between LogEntity and LogDto.
 *
 * <p>@Mapper(componentModel = "spring") makes this a Spring bean that can be injected.
 *
 * <p>MapStruct generates the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface LogMapper {

    /**
     * Convert LogEntity to LogDto.
     *
     * <p>@Mapping maps nested network.id and device.id to flat fields.
     */
    @Mapping(source = "network.id", target = "networkId")
    @Mapping(source = "device.id", target = "deviceId")
    LogDto toDto(LogEntity entity);

    /**
     * Convert list of LogEntity to list of LogDto.
     *
     * <p>MapStruct automatically generates this using toDto() for each element.
     */
    List<LogDto> toDtos(List<LogEntity> entities);

    /**
     * Convert Page of LogEntity to Page of LogDto.
     *
     * <p>Default method allows custom logic for Page mapping.
     */
    default Page<LogDto> toDtoPage(Page<LogEntity> page) {
        return page.map(this::toDto);
    }
}
