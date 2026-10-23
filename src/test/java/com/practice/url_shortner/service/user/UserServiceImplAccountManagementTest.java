package com.practice.url_shortner.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.ChangePasswordRequestRecord;
import com.practice.url_shortner.model.CreateUserRequest;
import com.practice.url_shortner.model.PasswordResetRequestRecord;
import com.practice.url_shortner.model.RecoveryCodeResponseRecord;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.service.token.IRefreshTokenService;
import com.practice.url_shortner.service.token.ITokenRevocationService;
import com.practice.url_shortner.testsupport.TestDataFactory;
import com.practice.url_shortner.utility.JwtUtils;
import com.practice.url_shortner.utility.RecoveryCodeUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAccountManagementTest {

	private static final int USER_ID = 42;

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
	private PasswordEncoder passwordEncoder;

	@Mock
	private PasswordPolicy passwordPolicy;

	private UserServiceImpl userService;

	private UserEntity user;

	@BeforeEach
	void setUp() {
		userService = new UserServiceImpl(authenticationManager, jwtUtils, tokenRevocationService,
				refreshTokenService, userRepository, userDetailsManager, passwordEncoder, passwordPolicy);
		user = TestDataFactory.aUserEntity(u -> {
			u.setId(USER_ID);
			u.setPassword("stored-hash");
		});
	}

	private void assertErrorCode(Runnable action, ErrorCode expected) {
		assertThatThrownBy(action::run).isInstanceOf(CustomException.class)
				.extracting(ex -> ((CustomException) ex).getErrorCode()).isEqualTo(expected);
	}

	private void userIsLockedAndPasswordMatches(boolean matches) {
		when(userRepository.findByUsernameForUpdate("test-user")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("current-password", "stored-hash")).thenReturn(matches);
	}

	// ---- register ----

	@Test
	void register_createsTheUserWithAnEncodedPassword_andReturnsARecoveryCodeWhoseHashIsStored() {
		when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");

		RecoveryCodeResponseRecord response = userService.register(new CreateUserRequest("test-user", "test-password"));

		verify(passwordPolicy).validate("test-password");
		ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
		verify(userDetailsManager).createUser(created.capture(), storedHash.capture());
		assertThat(created.getValue().getUsername()).isEqualTo("test-user");
		assertThat(created.getValue().getPassword()).isEqualTo("encoded-password");
		assertThat(response.recoveryCode()).matches("([A-Z2-7]{4}-){5}[A-Z2-7]{4}");
		assertThat(storedHash.getValue()).isEqualTo(RecoveryCodeUtils.hash(response.recoveryCode()));
	}

	@Test
	void register_createsNothing_whenThePasswordBreaksThePolicy() {
		doThrow(CustomException.builder().errorCode(ErrorCode.WEAK_PASSWORD).build()).when(passwordPolicy)
				.validate("weak");

		assertErrorCode(() -> userService.register(new CreateUserRequest("test-user", "weak")),
				ErrorCode.WEAK_PASSWORD);

		verifyNoInteractions(userDetailsManager, userRepository);
	}

	@Test
	void register_propagatesUsernameNotAvailable_andIssuesNoCode() {
		when(passwordEncoder.encode(any())).thenReturn("encoded-password");
		doThrow(CustomException.builder().errorCode(ErrorCode.USER_NAME_NOT_AVAILABLE).build())
				.when(userDetailsManager).createUser(any(), any());

		assertErrorCode(() -> userService.register(new CreateUserRequest("test-user", "test-password")),
				ErrorCode.USER_NAME_NOT_AVAILABLE);

		verify(userRepository, never()).save(any());
	}

	// ---- changePassword ----

	@Test
	void changePassword_storesTheNewEncodedPassword_andEndsAllSessions() {
		userIsLockedAndPasswordMatches(true);
		when(passwordEncoder.matches("new-password", "stored-hash")).thenReturn(false);
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		userService.changePassword("test-user", new ChangePasswordRequestRecord("current-password", "new-password"));

		assertThat(user.getPassword()).isEqualTo("new-hash");
		verify(passwordPolicy).validate("new-password");
		verify(refreshTokenService).revokeAllSessions(USER_ID);
	}

	@Test
	void changePassword_rejectsAWrongCurrentPassword_andChangesNothing() {
		userIsLockedAndPasswordMatches(false);

		assertErrorCode(() -> userService.changePassword("test-user",
				new ChangePasswordRequestRecord("current-password", "new-password")), ErrorCode.INVALID_CREDENTIALS);

		assertThat(user.getPassword()).isEqualTo("stored-hash");
		verify(userRepository, never()).save(any());
		verifyNoInteractions(refreshTokenService, passwordPolicy);
	}

	@Test
	void changePassword_rejectsAPasswordThatBreaksThePolicy() {
		userIsLockedAndPasswordMatches(true);
		doThrow(CustomException.builder().errorCode(ErrorCode.WEAK_PASSWORD).build()).when(passwordPolicy)
				.validate("weak");

		assertErrorCode(
				() -> userService.changePassword("test-user", new ChangePasswordRequestRecord("current-password", "weak")),
				ErrorCode.WEAK_PASSWORD);

		verify(userRepository, never()).save(any());
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void changePassword_rejectsTheCurrentPasswordAsTheNewOne() {
		userIsLockedAndPasswordMatches(true);
		when(passwordEncoder.matches("new-password", "stored-hash")).thenReturn(true);

		assertErrorCode(() -> userService.changePassword("test-user",
				new ChangePasswordRequestRecord("current-password", "new-password")), ErrorCode.PASSWORD_UNCHANGED);

		verify(userRepository, never()).save(any());
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void changePassword_throwsUserNotFound_whenTheUserIsGone() {
		when(userRepository.findByUsernameForUpdate("test-user")).thenReturn(Optional.empty());

		assertErrorCode(() -> userService.changePassword("test-user",
				new ChangePasswordRequestRecord("current-password", "new-password")), ErrorCode.USER_NOT_FOUND);
	}

	// ---- deactivate ----

	@Test
	void deactivate_disablesTheUser_andEndsAllSessions() {
		userIsLockedAndPasswordMatches(true);

		userService.deactivate("test-user", "current-password");

		assertThat(user.isEnabled()).isFalse();
		verify(userRepository).save(user);
		verify(refreshTokenService).revokeAllSessions(USER_ID);
	}

	@Test
	void deactivate_rejectsAWrongPassword_andLeavesTheUserEnabled() {
		userIsLockedAndPasswordMatches(false);

		assertErrorCode(() -> userService.deactivate("test-user", "current-password"), ErrorCode.INVALID_CREDENTIALS);

		assertThat(user.isEnabled()).isTrue();
		verify(userRepository, never()).save(any());
		verify(refreshTokenService, never()).revokeAllSessions(anyInt());
	}

	// ---- regenerateRecoveryCode ----

	@Test
	void regenerateRecoveryCode_replacesTheStoredHash_andReturnsTheNewCode() {
		user.setRecoveryCodeHash("old-hash");
		userIsLockedAndPasswordMatches(true);

		RecoveryCodeResponseRecord response = userService.regenerateRecoveryCode("test-user", "current-password");

		assertThat(user.getRecoveryCodeHash()).isEqualTo(RecoveryCodeUtils.hash(response.recoveryCode()))
				.isNotEqualTo("old-hash");
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void regenerateRecoveryCode_rejectsAWrongPassword_andKeepsTheOldCode() {
		user.setRecoveryCodeHash("old-hash");
		userIsLockedAndPasswordMatches(false);

		assertErrorCode(() -> userService.regenerateRecoveryCode("test-user", "current-password"),
				ErrorCode.INVALID_CREDENTIALS);

		assertThat(user.getRecoveryCodeHash()).isEqualTo("old-hash");
	}

	// ---- resetPassword ----

	@Test
	void resetPassword_setsTheNewPassword_rotatesTheCode_andEndsAllSessions() {
		String code = RecoveryCodeUtils.generate();
		user.setRecoveryCodeHash(RecoveryCodeUtils.hash(code));
		when(userRepository.findByUsernameForUpdate("test-user")).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		RecoveryCodeResponseRecord response = userService
				.resetPassword(new PasswordResetRequestRecord("test-user", code, "new-password"));

		assertThat(user.getPassword()).isEqualTo("new-hash");
		assertThat(response.recoveryCode()).isNotEqualTo(code);
		assertThat(user.getRecoveryCodeHash()).isEqualTo(RecoveryCodeUtils.hash(response.recoveryCode()));
		verify(passwordPolicy).validate("new-password");
		verify(refreshTokenService).revokeAllSessions(USER_ID);
	}

	@Test
	void resetPassword_acceptsTheCodeTypedInLowerCaseWithoutDashes() {
		String code = RecoveryCodeUtils.generate();
		user.setRecoveryCodeHash(RecoveryCodeUtils.hash(code));
		when(userRepository.findByUsernameForUpdate("test-user")).thenReturn(Optional.of(user));
		when(passwordEncoder.encode(any())).thenReturn("new-hash");

		userService.resetPassword(
				new PasswordResetRequestRecord("test-user", code.replace("-", "").toLowerCase(), "new-password"));

		assertThat(user.getPassword()).isEqualTo("new-hash");
	}

	@Test
	void resetPassword_rejectsAWrongCode_andChangesNothing() {
		user.setRecoveryCodeHash(RecoveryCodeUtils.hash(RecoveryCodeUtils.generate()));
		when(userRepository.findByUsernameForUpdate("test-user")).thenReturn(Optional.of(user));

		assertErrorCode(() -> userService.resetPassword(
				new PasswordResetRequestRecord("test-user", RecoveryCodeUtils.generate(), "new-password")),
				ErrorCode.INVALID_CREDENTIALS);

		assertThat(user.getPassword()).isEqualTo("stored-hash");
		verify(userRepository, never()).save(any());
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void resetPassword_rejectsAnUnknownUser_withTheSameErrorAsAWrongCode() {
		when(userRepository.findByUsernameForUpdate("ghost")).thenReturn(Optional.empty());

		assertErrorCode(() -> userService
				.resetPassword(new PasswordResetRequestRecord("ghost", RecoveryCodeUtils.generate(), "new-password")),
				ErrorCode.INVALID_CREDENTIALS);

		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void resetPassword_rejectsAUserWhoNeverGotARecoveryCode() {
		user.setRecoveryCodeHash(null);
		when(userRepository.findByUsernameForUpdate("test-user")).thenReturn(Optional.of(user));

		assertErrorCode(() -> userService.resetPassword(
				new PasswordResetRequestRecord("test-user", RecoveryCodeUtils.generate(), "new-password")),
				ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	void resetPassword_rejectsADisabledUser_evenWithTheRightCode() {
		String code = RecoveryCodeUtils.generate();
		user.setRecoveryCodeHash(RecoveryCodeUtils.hash(code));
		user.setEnabled(false);
		when(userRepository.findByUsernameForUpdate("test-user")).thenReturn(Optional.of(user));

		assertErrorCode(
				() -> userService.resetPassword(new PasswordResetRequestRecord("test-user", code, "new-password")),
				ErrorCode.INVALID_CREDENTIALS);

		assertThat(user.getPassword()).isEqualTo("stored-hash");
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void resetPassword_rejectsAPasswordThatBreaksThePolicy_beforeLookingUpTheUser() {
		doThrow(CustomException.builder().errorCode(ErrorCode.WEAK_PASSWORD).build()).when(passwordPolicy)
				.validate("weak");

		assertErrorCode(() -> userService
				.resetPassword(new PasswordResetRequestRecord("test-user", RecoveryCodeUtils.generate(), "weak")),
				ErrorCode.WEAK_PASSWORD);

		verifyNoInteractions(userRepository);
	}
}
