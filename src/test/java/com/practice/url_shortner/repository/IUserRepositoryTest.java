package com.practice.url_shortner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.testsupport.TestDataFactory;

@DataJpaTest
@ActiveProfiles("test")
class IUserRepositoryTest {

	@Autowired
	private IUserRepository userRepository;

	@Test
	void findByUsername_returnsUser_whenExists() {
		userRepository.save(TestDataFactory.aUserEntity(u -> u.setUsername("existing-user")));

		Optional<UserEntity> found = userRepository.findByUsername("existing-user");

		assertThat(found).isPresent();
		assertThat(found.get().getUsername()).isEqualTo("existing-user");
	}

	@Test
	void findByUsername_returnsEmpty_whenNotExists() {
		Optional<UserEntity> found = userRepository.findByUsername("no-such-user");

		assertThat(found).isEmpty();
	}

	@Test
	void userExists_returnsTrue_whenUsernamePresent() {
		userRepository.save(TestDataFactory.aUserEntity(u -> u.setUsername("existing-user")));

		assertThat(userRepository.userExists("existing-user")).isTrue();
	}

	@Test
	void userExists_returnsFalse_whenUsernameAbsent() {
		assertThat(userRepository.userExists("no-such-user")).isFalse();
	}
}
