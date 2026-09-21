package com.practice.url_shortner.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.userdetails.User;

import com.practice.url_shortner.entity.UserEntity;

@Mapper(componentModel = ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface IUserDetailsToUserEntityMapper extends Converter<User, UserEntity> {

	@Override
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "id", ignore = true)
	UserEntity convert(User user);
}
