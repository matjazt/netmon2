package com.matjazt.netmon2.mapper;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.request.SaveAccountRequest;
import com.matjazt.netmon2.entity.AccountEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for converting between AccountEntity and AccountDto.
 *
 * <p>@Mapper(componentModel = "spring") makes this a Spring bean that can be injected.
 *
 * <p>MapStruct generates the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    /**
     * Convert AccountEntity to AccountDto.
     *
     * <p>@Mapping maps nested accountType.name to flat accountTypeName field.
     */
    @Mapping(source = "accountType.name", target = "accountTypeName")
    AccountDto toDto(AccountEntity entity);

    @Mapping(source = "accountTypeId", target = "accountType.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    AccountEntity toEntity(SaveAccountRequest request);

    /**
     * Convert list of AccountEntity to list of AccountDto.
     *
     * <p>MapStruct automatically generates this using toDto() for each element.
     */
    List<AccountDto> toDtos(List<AccountEntity> entities);

    /**
     * Convert Page of AccountEntity to Page of AccountDto.
     *
     * <p>Default method allows custom logic for Page mapping.
     */
    default Page<AccountDto> toDtoPage(Page<AccountEntity> page) {
        return page.map(this::toDto);
    }
}
