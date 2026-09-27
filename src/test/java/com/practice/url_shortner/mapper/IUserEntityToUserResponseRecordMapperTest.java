package com.practice.url_shortner.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.model.UserResponseRecord;
import com.practice.url_shortner.testsupport.TestDataFactory;

class IUserEntityToUserResponseRecordMapperTest {

	private final IUserEntityToUserResponseRecordMapperImpl mapper = new IUserEntityToUserResponseRecordMapperImpl();

	@Test
	void convert_mapsUsernameToUserNameField() {
		UserEntity userEntity = TestDataFactory.aUserEntity(u -> {
			u.setId(7);
			u.setUsername("mapped-user");
		});

		UserResponseRecord response = mapper.convert(userEntity);

		assertThat(response.userName()).isEqualTo(userEntity.getUsername());
		assertThat(response.id()).isEqualTo(7);
	}
}
