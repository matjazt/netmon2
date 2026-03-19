package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.request.SaveNetworkRequest;
import com.matjazt.netmon2.entity.NetworkEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * MapStruct mapper for converting between NetworkEntity and NetworkDto.
 *
 * <p>@Mapper(componentModel = "spring") makes this a Spring bean that can be injected.
 *
 * <p>MapStruct generates the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface NetworkMapper {

    /**
     * Convert NetworkEntity to NetworkDto.
     *
     * <p>Fields match 1:1, so no explicit @Mapping needed.
     */
    NetworkDto toDto(NetworkEntity entity);

    @Mapping(target = "id", ignore = true)
    NetworkEntity toEntity(SaveNetworkRequest request);

    /**
     * Convert list of NetworkEntity to list of NetworkDto.
     *
     * <p>MapStruct automatically generates this using toDto() for each element.
     */
    List<NetworkDto> toDtos(List<NetworkEntity> entities);

    default Instant toInstant(LocalDateTime ldt) {
        return ldt != null ? ldt.toInstant(ZoneOffset.UTC) : null;
    }
}
