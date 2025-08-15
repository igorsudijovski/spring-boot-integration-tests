package com.igorsudijovski.integrationtests.mapper;

import com.igorsudijovski.integrationtests.client.model.UserDto;
import com.igorsudijovski.integrationtests.model.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(UserEntity userEntity);
    UserEntity toEntity(UserDto userDto);

}

