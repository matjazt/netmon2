package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.response.AccountResponseDto;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between AccountDto and AccountResponseDto.
 *
 * <p>This mapper is used in the controller layer to convert domain DTOs to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface AccountApiMapper {

    /**
     * Convert AccountDto to AccountResponseDto.
     *
     * <p>Fields match 1:1, so no explicit @Mapping needed.
     */
    AccountResponseDto toResponse(AccountDto dto);

    /** Convert list of AccountDto to list of AccountResponseDto. */
    List<AccountResponseDto> toResponses(List<AccountDto> dtos);

    /** Convert Page of AccountDto to Page of AccountResponseDto. */
    default Page<AccountResponseDto> toResponsePage(Page<AccountDto> page) {
        return page.map(this::toResponse);
    }
}
