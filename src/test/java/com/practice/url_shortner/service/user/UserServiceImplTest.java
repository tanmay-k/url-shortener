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

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.service.token.IRefreshTokenService;
import com.practice.url_shortner.service.token.IRefreshTokenService.IssuedSession;
import com.practice.url_shortner.service.token.ITokenRevocationService;
import com.practice.url_shortner.utility.JwtUtils;
import com.auth0.jwt.interfaces.DecodedJWT;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private ITokenRevocationService tokenRevocationService;

	@Mock
	private IRefreshTokenService refreshTokenService;

	@Mock
	private IUserRepository userRepository;

	@Mock
	private UserDetailsManagerImpl userDetailsManager;

	@Mock
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@Mock
	private PasswordPolicy passwordPolicy;

	private UserServiceImpl userService;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		userService = new UserServiceImpl(authenticationManager, jwtUtils, tokenRevocationService, refreshTokenService,
				userRepository, userDetailsManager, passwordEncoder, passwordPolicy);
	}

	@Test
	void login_authenticatesAndReturnsAccessAndRefreshTokens() {
		when(refreshTokenService.startSession("test-user"))
				.thenReturn(new IssuedSession("test-user", "refresh-1", "jti-1"));
		when(jwtUtils.createToken("test-user", "jti-1")).thenReturn("fixed-token");
		when(jwtUtils.accessTokenValidity()).thenReturn(java.time.Duration.ofMinutes(15));

		AuthenticationResponseRecord response = userService
				.login(new AuthenticationRequestRecord("test-user", "test-password"));

		assertThat(response.token()).isEqualTo("fixed-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-1");
		assertThat(response.expiresIn()).isEqualTo(900);
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
		when(refreshTokenService.startSession(any()))
				.thenReturn(new IssuedSession("test-user", "refresh-1", "jti-1"));
		when(jwtUtils.accessTokenValidity()).thenReturn(java.time.Duration.ofMinutes(15));

		userService.login(new AuthenticationRequestRecord("test-user", "test-password"));

		ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor
				.forClass(UsernamePasswordAuthenticationToken.class);
		verify(authenticationManager).authenticate(captor.capture());
		assertThat(captor.getValue().getPrincipal()).isEqualTo("test-user");
		assertThat(captor.getValue().getCredentials()).isEqualTo("test-password");
	}

	@Test
	void logout_revokesTheVerifiedToken() {
		DecodedJWT decodedJWT = org.mockito.Mockito.mock(DecodedJWT.class);
		when(decodedJWT.getId()).thenReturn("jti-1");
		when(jwtUtils.verifyJwt("a-token")).thenReturn(decodedJWT);

		userService.logout("a-token");

		verify(tokenRevocationService).revoke(decodedJWT);
		verify(refreshTokenService).revokeSessionByAccessJti("jti-1");
	}

	@Test
	void login_doesNotStartSession_whenAuthenticationFails() {
		org.mockito.Mockito.doThrow(new BadCredentialsException("bad creds")).when(authenticationManager)
				.authenticate(any());

		assertThatThrownBy(() -> userService.login(new AuthenticationRequestRecord("test-user", "wrong-password")))
				.isInstanceOf(BadCredentialsException.class);

		org.mockito.Mockito.verifyNoInteractions(refreshTokenService);
	}

	@Test
	void refresh_rotatesTheTokenAndIssuesNewAccessToken() {
		when(refreshTokenService.rotate("old-refresh"))
				.thenReturn(new IssuedSession("test-user", "new-refresh", "jti-2"));
		when(jwtUtils.createToken("test-user", "jti-2")).thenReturn("new-access");
		when(jwtUtils.accessTokenValidity()).thenReturn(java.time.Duration.ofMinutes(15));

		AuthenticationResponseRecord response = userService.refresh("old-refresh");

		assertThat(response.token()).isEqualTo("new-access");
		assertThat(response.refreshToken()).isEqualTo("new-refresh");
		assertThat(response.expiresIn()).isEqualTo(900);
	}

	@Test
	void getUser_throwsIllegalArgumentException_whenUsernameBlank() {
		assertThatThrownBy(() -> userService.getUser("")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void getUser_throwsUserNotFound_whenAbsent() {
		when(userRepository.findByUsername("test-user")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUser("test-user")).isInstanceOf(CustomException.class)
				.extracting(ex -> ((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void getUserById_currentlyNoOp() {
		userService.getUserById(1);
	}
}
