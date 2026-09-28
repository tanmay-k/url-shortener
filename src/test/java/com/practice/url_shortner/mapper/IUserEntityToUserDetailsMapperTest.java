package com.practice.url_shortner.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.testsupport.TestDataFactory;

class IUserEntityToUserDetailsMapperTest {

	private final IUserEntityToUserDetailsMapperImpl mapper = new IUserEntityToUserDetailsMapperImpl();

	@Test
	void convert_mapsUsernamePasswordAndFlagsCorrectly() {
		UserEntity userEntity = TestDataFactory.aUserEntity(u -> {
			u.setUsername("mapped-user");
			u.setPassword("mapped-password");
			u.setEnabled(true);
			u.setAccountNonExpired(true);
			u.setAccountNonLocked(true);
			u.setCredentialsNonExpired(true);
		});

		User user = mapper.convert(userEntity);

		assertThat(user.getUsername()).isEqualTo("mapped-user");
		assertThat(user.getPassword()).isEqualTo("mapped-password");
		assertThat(user.isEnabled()).isTrue();
		assertThat(user.isAccountNonExpired()).isTrue();
		assertThat(user.isAccountNonLocked()).isTrue();
		assertThat(user.isCredentialsNonExpired()).isTrue();
	}

	@Test
	void convert_authoritiesAreEmpty() {
		UserEntity userEntity = TestDataFactory.aUserEntity();

		User user = mapper.convert(userEntity);

		assertThat(user.getAuthorities()).isEmpty();
	}
}
