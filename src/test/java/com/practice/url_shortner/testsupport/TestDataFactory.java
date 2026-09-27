package com.practice.url_shortner.testsupport;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.practice.url_shortner.entity.UrlMappingEntity;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.CreateUserRequest;
import com.practice.url_shortner.model.NewUrlMappingRequestRecord;

public final class TestDataFactory {

	private TestDataFactory() {
	}

	public static UserEntity aUserEntity() {
		return aUserEntity(userEntity -> {
		});
	}

	public static UserEntity aUserEntity(Consumer<UserEntity> customizer) {
		UserEntity userEntity = new UserEntity();
		userEntity.setUsername("test-user");
		userEntity.setPassword("encoded-password");
		userEntity.setAccountNonExpired(true);
		userEntity.setAccountNonLocked(true);
		userEntity.setCredentialsNonExpired(true);
		userEntity.setEnabled(true);
		LocalDateTime now = LocalDateTime.now();
		userEntity.setCreatedAt(now);
		userEntity.setUpdatedAt(now);
		customizer.accept(userEntity);
		return userEntity;
	}

	public static UrlMappingEntity aUrlMappingEntity() {
		return aUrlMappingEntity(urlMappingEntity -> {
		});
	}

	public static UrlMappingEntity aUrlMappingEntity(Consumer<UrlMappingEntity> customizer) {
		UrlMappingEntity urlMappingEntity = new UrlMappingEntity();
		urlMappingEntity.setShortCode("abc12345");
		urlMappingEntity.setLongUrl("https://93.184.216.34/");
		urlMappingEntity.setEnabled(true);
		LocalDateTime now = LocalDateTime.now();
		urlMappingEntity.setCreatedAt(now);
		urlMappingEntity.setUpdatedAt(now);
		customizer.accept(urlMappingEntity);
		return urlMappingEntity;
	}

	public static AuthenticationRequestRecord aLoginRequest() {
		return new AuthenticationRequestRecord("test-user", "test-password");
	}

	public static CreateUserRequest aCreateUserRequest() {
		return new CreateUserRequest("test-user", "test-password");
	}

	public static NewUrlMappingRequestRecord aValidUrlMappingRequest() {
		return new NewUrlMappingRequestRecord("https://93.184.216.34/");
	}
}
