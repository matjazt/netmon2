package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.DeviceStatusHistoryDto;
import com.matjazt.netmon2.dto.response.DeviceStatusHistoryResponseDto;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between DeviceStatusHistoryDto and
 * DeviceStatusHistoryResponseDto.
 *
 * <p>This mapper is used in the controller layer to convert domain DTOs to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface DeviceStatusHistoryApiMapper {

    /**
     * Convert DeviceStatusHistoryDto to DeviceStatusHistoryResponseDto.
     *
     * <p>Fields match 1:1, so no explicit @Mapping needed.
     */
    DeviceStatusHistoryResponseDto toResponse(DeviceStatusHistoryDto dto);

    /** Convert list of DeviceStatusHistoryDto to list of DeviceStatusHistoryResponseDto. */
    List<DeviceStatusHistoryResponseDto> toResponses(List<DeviceStatusHistoryDto> dtos);

    /** Convert Page of DeviceStatusHistoryDto to Page of DeviceStatusHistoryResponseDto. */
    default Page<DeviceStatusHistoryResponseDto> toResponsePage(Page<DeviceStatusHistoryDto> page) {
        return page.map(this::toResponse);
    }
}
