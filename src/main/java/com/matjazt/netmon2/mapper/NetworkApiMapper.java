package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.response.NetworkResponseDto;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between NetworkDto and NetworkResponseDto.
 *
 * <p>This mapper is used in the controller layer to convert domain DTOs to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface NetworkApiMapper {

    /**
     * Convert NetworkDto to NetworkResponseDto.
     *
     * <p>Fields match 1:1, so no explicit @Mapping needed.
     */
    NetworkResponseDto toResponse(NetworkDto dto);

    /** Convert list of NetworkDto to list of NetworkResponseDto. */
    List<NetworkResponseDto> toResponses(List<NetworkDto> dtos);

    /** Convert Page of NetworkDto to Page of NetworkResponseDto. */
    default Page<NetworkResponseDto> toResponsePage(Page<NetworkDto> page) {
        return page.map(this::toResponse);
    }
}
