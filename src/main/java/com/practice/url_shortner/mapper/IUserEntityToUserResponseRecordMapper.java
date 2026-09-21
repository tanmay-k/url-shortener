package com.practice.url_shortner.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.springframework.core.convert.converter.Converter;

import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.model.UserResponseRecord;

@Mapper(componentModel = ComponentModel.SPRING)
public interface IUserEntityToUserResponseRecordMapper extends Converter<UserEntity, UserResponseRecord> {

	@Mapping(source = "username", target = "userName")
	@Override
	UserResponseRecord convert(UserEntity source);
}
