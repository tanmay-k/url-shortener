package com.practice.url_shortner.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import com.practice.url_shortner.entity.UserEntity;

class IUserDetailsToUserEntityMapperTest {

	private final IUserDetailsToUserEntityMapperImpl mapper = new IUserDetailsToUserEntityMapperImpl();

	@Test
	void convert_mapsUsernameAndPassword() {
		User user = new User("mapped-user", "mapped-password", true, true, true, true, Collections.emptyList());

		UserEntity userEntity = mapper.convert(user);

		assertThat(userEntity.getUsername()).isEqualTo("mapped-user");
		assertThat(userEntity.getPassword()).isEqualTo("mapped-password");
	}

	@Test
	void convert_ignoresIdCreatedAtUpdatedAt() {
		User user = new User("mapped-user", "mapped-password", Collections.emptyList());

		UserEntity userEntity = mapper.convert(user);

		assertThat(userEntity.getId()).isZero();
		assertThat(userEntity.getCreatedAt()).isNull();
		assertThat(userEntity.getUpdatedAt()).isNull();
	}
}
