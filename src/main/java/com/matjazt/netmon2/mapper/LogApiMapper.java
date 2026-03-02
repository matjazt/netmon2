package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.LogDto;
import com.matjazt.netmon2.dto.response.LogResponseDto;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between LogDto and LogResponseDto.
 *
 * <p>This mapper is used in the controller layer to convert domain DTOs to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface LogApiMapper {

    /**
     * Convert LogDto to LogResponseDto.
     *
     * <p>Fields match 1:1, so no explicit @Mapping needed.
     */
    LogResponseDto toResponse(LogDto dto);

    /** Convert list of LogDto to list of LogResponseDto. */
    List<LogResponseDto> toResponses(List<LogDto> dtos);

    /** Convert Page of LogDto to Page of LogResponseDto. */
    default Page<LogResponseDto> toResponsePage(Page<LogDto> page) {
        return page.map(this::toResponse);
    }
}
