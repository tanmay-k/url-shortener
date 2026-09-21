package com.practice.url_shortner.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.practice.url_shortner.entity.UserEntity;

@Mapper(componentModel = ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface IUserEntityToUserDetailsMapper extends Converter<UserEntity, User> {

//	@Mapping(source = "createdAt", ignore = true)
//	@Mapping(target = "username", source = "userEntity.username")
//    @Mapping(target = "password", source = "userEntity.password")
//    @Mapping(target = "accountNonExpired", source = "userEntity.accountNonExpired")
//    @Mapping(target = "credentialsNonExpired", source = "userEntity.credentialsNonExpired")
//    @Mapping(target = "accountNonLocked", source = "userEntity.accountNonLocked")
    @Mapping(target = "authorities", ignore = true)
	User convert(UserEntity userEntity);
	
	@ObjectFactory
	default User toUser(UserEntity userEntity)
	{
		return new User(userEntity.getUsername(), userEntity.getPassword(), userEntity.isEnabled(),
				userEntity.isAccountNonExpired(), userEntity.isCredentialsNonExpired(), userEntity.isAccountNonLocked(),
				Collections.emptyList());
	}
	
	@Named("mapRolesToAuthorities")
    default Collection<? extends GrantedAuthority> mapRolesToAuthorities(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
