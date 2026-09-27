package com.practice.url_shortner.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.utility.JwtUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtUtils jwtUtils;

	private UserServiceImpl userService;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		userService = new UserServiceImpl(authenticationManager, jwtUtils);
	}

	@Test
	void login_authenticatesAndReturnsTokenFromJwtUtils() {
		when(jwtUtils.createToken("test-user")).thenReturn("fixed-token");

		AuthenticationResponseRecord response = userService
				.login(new AuthenticationRequestRecord("test-user", "test-password"));

		assertThat(response.token()).isEqualTo("fixed-token");
		assertThat(response.refreshToken()).isNull();
	}

	@Test
	void login_propagatesBadCredentialsException_whenAuthenticationManagerThrows() {
		org.mockito.Mockito.doThrow(new BadCredentialsException("bad creds")).when(authenticationManager)
				.authenticate(any());

		assertThatThrownBy(() -> userService.login(new AuthenticationRequestRecord("test-user", "wrong-password")))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void login_callsAuthenticationManagerWithCorrectCredentials() {
		when(jwtUtils.createToken(any())).thenReturn("token");

		userService.login(new AuthenticationRequestRecord("test-user", "test-password"));

		ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor
				.forClass(UsernamePasswordAuthenticationToken.class);
		verify(authenticationManager).authenticate(captor.capture());
		assertThat(captor.getValue().getPrincipal()).isEqualTo("test-user");
		assertThat(captor.getValue().getCredentials()).isEqualTo("test-password");
	}

	@Test
	void getUser_throwsNpe_becauseUserRepositoryIsNotConstructorInjected() {
		// UserServiceImpl.userRepository is not declared `final`, so
		// @RequiredArgsConstructor never wires it - it stays null after
		// construction via the real constructor. This test pins that current
		// (buggy) behavior rather than working around it.
		assertThatThrownBy(() -> userService.getUser("test-user")).isInstanceOf(NullPointerException.class);
	}

	@Test
	void getUser_throwsIllegalArgumentException_whenUsernameBlank() {
		assertThatThrownBy(() -> userService.getUser("")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void getUser_withReflectivelyInjectedRepository_throwsUserNotFound_whenAbsent() {
		IUserRepository userRepository = org.mockito.Mockito.mock(IUserRepository.class);
		ReflectionTestUtils.setField(userService, "userRepository", userRepository);
		when(userRepository.findByUsername("test-user")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUser("test-user")).isInstanceOf(CustomException.class)
				.extracting(ex -> ((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void getUserById_currentlyNoOp() {
		userService.getUserById(1);
	}
}
