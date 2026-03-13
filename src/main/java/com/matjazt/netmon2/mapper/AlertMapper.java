package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.AlertDto;
import com.matjazt.netmon2.entity.AlertEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/** MapStruct mapper for converting between AlertEntity and AlertDto. */
@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(source = "network.id", target = "networkId")
    @Mapping(source = "device.id", target = "deviceId")
    @Mapping(target = "alertType", source = "alertType")
    AlertDto toDto(AlertEntity entity);

    List<AlertDto> toDtos(List<AlertEntity> entities);
}
