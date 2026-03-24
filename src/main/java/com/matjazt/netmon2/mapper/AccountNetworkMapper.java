package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.AccountNetworkDto;
import com.matjazt.netmon2.entity.AccountNetworkEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between AccountNetworkEntity and AccountNetworkDto.
 *
 * <p>@Mapper(componentModel = "spring") makes this a Spring bean that can be injected.
 *
 * <p>MapStruct generates the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface AccountNetworkMapper {

    /**
     * Convert AccountNetworkEntity to AccountNetworkDto.
     *
     * <p>@Mapping maps nested account.id and network.id to flat fields.
     */
    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "network.id", target = "networkId")
    AccountNetworkDto toDto(AccountNetworkEntity entity);

    /**
     * Convert list of AccountNetworkEntity to list of AccountNetworkDto.
     *
     * <p>MapStruct automatically generates this using toDto() for each element.
     */
    List<AccountNetworkDto> toDtos(List<AccountNetworkEntity> entities);
}
