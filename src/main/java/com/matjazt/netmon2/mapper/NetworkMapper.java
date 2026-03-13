package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.request.SaveNetworkRequest;
import com.matjazt.netmon2.entity.NetworkEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

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

    /**
     * Convert Page of NetworkEntity to Page of NetworkDto.
     *
     * <p>Default method allows custom logic for Page mapping.
     */
    default Page<NetworkDto> toDtoPage(Page<NetworkEntity> page) {
        return page.map(this::toDto);
    }
}
