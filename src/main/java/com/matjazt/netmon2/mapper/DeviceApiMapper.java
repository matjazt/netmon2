package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.response.DeviceResponseDto;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between DeviceDto and DeviceResponseDto.
 *
 * <p>This mapper is used in the controller layer to convert domain DTOs to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface DeviceApiMapper {

    /**
     * Convert DeviceDto to DeviceResponseDto.
     *
     * <p>Fields match 1:1, so no explicit @Mapping needed.
     */
    DeviceResponseDto toResponse(DeviceDto dto);

    /** Convert list of DeviceDto to list of DeviceResponseDto. */
    List<DeviceResponseDto> toResponses(List<DeviceDto> dtos);

    /** Convert Page of DeviceDto to Page of DeviceResponseDto. */
    default Page<DeviceResponseDto> toResponsePage(Page<DeviceDto> page) {
        return page.map(this::toResponse);
    }
}
