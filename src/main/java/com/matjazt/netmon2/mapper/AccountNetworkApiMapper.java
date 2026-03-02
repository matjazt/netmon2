package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.AccountNetworkDto;
import com.matjazt.netmon2.dto.response.AccountNetworkResponseDto;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between AccountNetworkDto and AccountNetworkResponseDto.
 *
 * <p>This mapper is used in the controller layer to convert domain DTOs to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface AccountNetworkApiMapper {

    /**
     * Convert AccountNetworkDto to AccountNetworkResponseDto.
     *
     * <p>Fields match 1:1, so no explicit @Mapping needed.
     */
    AccountNetworkResponseDto toResponse(AccountNetworkDto dto);

    /** Convert list of AccountNetworkDto to list of AccountNetworkResponseDto. */
    List<AccountNetworkResponseDto> toResponses(List<AccountNetworkDto> dtos);

    /** Convert Page of AccountNetworkDto to Page of AccountNetworkResponseDto. */
    default Page<AccountNetworkResponseDto> toResponsePage(Page<AccountNetworkDto> page) {
        return page.map(this::toResponse);
    }
}
