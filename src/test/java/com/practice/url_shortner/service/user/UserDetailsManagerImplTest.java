package com.practice.url_shortner.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.mapper.IUserDetailsToUserEntityMapper;
import com.practice.url_shortner.mapper.IUserEntityToUserDetailsMapper;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.testsupport.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class UserDetailsManagerImplTest {

	@Mock
	private IUserRepository userRepository;

	@Mock
	private IUserEntityToUserDetailsMapper userToUserDetailsMapper;

	@Mock
	private IUserDetailsToUserEntityMapper userDetailsToUserMapper;

	private UserDetailsManagerImpl userDetailsManager;

	@BeforeEach
	void setUp() {
		userDetailsManager = new UserDetailsManagerImpl(userRepository, userToUserDetailsMapper,
				userDetailsToUserMapper);
	}

	@Test
	void loadUserByUsername_returnsMappedUserDetails_whenFound() {
		UserEntity userEntity = TestDataFactory.aUserEntity();
		User userDetails = new User("test-user", "encoded-password", java.util.Collections.emptyList());
		when(userRepository.findByUsername("test-user")).thenReturn(Optional.of(userEntity));
		when(userToUserDetailsMapper.convert(userEntity)).thenReturn(userDetails);

		UserDetails result = userDetailsManager.loadUserByUsername("test-user");

		assertThat(result).isEqualTo(userDetails);
	}

	@Test
	void loadUserByUsername_throwsUsernameNotFoundException_whenBlank() {
		assertThatThrownBy(() -> userDetailsManager.loadUserByUsername(""))
				.isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void loadUserByUsername_throwsUsernameNotFoundException_whenNotFound() {
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsManager.loadUserByUsername("missing"))
				.isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void createUser_savesNewUserWithAuditFlagsSet() {
		User user = new User("test-user", "encoded-password", java.util.Collections.emptyList());
		when(userRepository.userExists("test-user")).thenReturn(false);
		UserEntity mappedEntity = new UserEntity();
		mappedEntity.setUsername("test-user");
		mappedEntity.setPassword("encoded-password");
		when(userDetailsToUserMapper.convert(user)).thenReturn(mappedEntity);
		when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userDetailsManager.createUser(user);

		org.mockito.ArgumentCaptor<UserEntity> captor = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
		verify(userRepository).save(captor.capture());
		UserEntity saved = captor.getValue();
		assertThat(saved.isAccountNonExpired()).isTrue();
		assertThat(saved.isAccountNonLocked()).isTrue();
		assertThat(saved.isCredentialsNonExpired()).isTrue();
		assertThat(saved.isEnabled()).isTrue();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	void createUser_throwsIllegalArgumentException_whenUsernameOrPasswordBlank() {
		User user = new User("test-user", "", true, true, true, true, java.util.Collections.emptyList());

		assertThatThrownBy(() -> userDetailsManager.createUser(user)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createUser_throwsCustomExceptionUserNameNotAvailable_whenUserExists() {
		User user = new User("test-user", "encoded-password", java.util.Collections.emptyList());
		when(userRepository.userExists("test-user")).thenReturn(true);

		assertThatThrownBy(() -> userDetailsManager.createUser(user)).isInstanceOf(CustomException.class)
				.extracting(ex -> ((CustomException) ex).getErrorCode())
				.isEqualTo(ErrorCode.USER_NAME_NOT_AVAILABLE);
	}

	@Test
	void userExists_returnsFalse_whenUsernameBlank() {
		boolean result = userDetailsManager.userExists("");

		assertThat(result).isFalse();
		verify(userRepository, never()).userExists(any());
	}

	@Test
	void userExists_delegatesToRepository_whenUsernamePresent() {
		when(userRepository.userExists("test-user")).thenReturn(true);

		boolean result = userDetailsManager.userExists("test-user");

		assertThat(result).isTrue();
	}

	@Test
	void updatePassword_returnsNull_currentlyNoOp() {
		User user = new User("test-user", "encoded-password", java.util.Collections.emptyList());

		UserDetails result = userDetailsManager.updatePassword(user, "new-password");

		assertThat(result).isNull();
	}

	@Test
	void updateUser_currentlyNoOp() {
		User user = new User("test-user", "encoded-password", java.util.Collections.emptyList());

		userDetailsManager.updateUser(user);
	}

	@Test
	void deleteUser_currentlyNoOp() {
		userDetailsManager.deleteUser("test-user");
	}

	@Test
	void changePassword_currentlyNoOp() {
		userDetailsManager.changePassword("old-password", "new-password");
	}
}
