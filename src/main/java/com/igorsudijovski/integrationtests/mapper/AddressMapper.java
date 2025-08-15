package com.igorsudijovski.integrationtests.mapper;

import com.igorsudijovski.integrationtests.client.model.AddressDto;
import com.igorsudijovski.integrationtests.model.AddressEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressDto toDto(AddressEntity addressEntity);

    AddressEntity toEntity(AddressDto addressDto);
}
