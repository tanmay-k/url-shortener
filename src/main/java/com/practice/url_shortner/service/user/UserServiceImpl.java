package com.practice.url_shortner.service.user;

import java.time.LocalDateTime;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.model.ChangePasswordRequestRecord;
import com.practice.url_shortner.model.CreateUserRequest;
import com.practice.url_shortner.model.PasswordResetRequestRecord;
import com.practice.url_shortner.model.RecoveryCodeResponseRecord;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.service.token.IRefreshTokenService;
import com.practice.url_shortner.service.token.IRefreshTokenService.IssuedSession;
import com.practice.url_shortner.service.token.ITokenRevocationService;
import com.practice.url_shortner.utility.JwtUtils;
import com.practice.url_shortner.utility.RecoveryCodeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

	private final AuthenticationManager authenticationManager;

	private final JwtUtils jwtUtils;

	private final ITokenRevocationService tokenRevocationService;

	private final IRefreshTokenService refreshTokenService;

	private final IUserRepository userRepository;

	private final UserDetailsManagerImpl userDetailsManager;

	private final PasswordEncoder passwordEncoder;

	private final PasswordPolicy passwordPolicy;

	@Override
	public AuthenticationResponseRecord login(AuthenticationRequestRecord credentials) {
		log.debug("Request received to authenticate user: {}", credentials.userName());
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(credentials.userName(), credentials.password()));
		log.debug("Authentication successful for user {}", credentials.userName());
		IssuedSession session = refreshTokenService.startSession(credentials.userName());
		log.info("New session started for user {}.", credentials.userName());
		return toResponse(session);
	}

	@Override
	public AuthenticationResponseRecord refresh(String refreshToken) {
		IssuedSession session = refreshTokenService.rotate(refreshToken);
		log.info("Session refreshed for user {}.", session.userName());
		return toResponse(session);
	}

	private AuthenticationResponseRecord toResponse(IssuedSession session) {
		String token = jwtUtils.createToken(session.userName(), session.accessJti());
		return new AuthenticationResponseRecord(token, session.refreshToken(),
				jwtUtils.accessTokenValidity().toSeconds());
	}

	@Override
	public void logout(String token) {
		DecodedJWT decodedJWT = jwtUtils.verifyJwt(token);
		tokenRevocationService.revoke(decodedJWT);
		refreshTokenService.revokeSessionByAccessJti(decodedJWT.getId());
		log.info("User {} logged out.", decodedJWT.getSubject());
	}

	@Override
	@Transactional
	public RecoveryCodeResponseRecord register(CreateUserRequest request) {
		passwordPolicy.validate(request.password());
		String recoveryCode = RecoveryCodeUtils.generate();
		userDetailsManager.createUser(
				new User(request.userName(), passwordEncoder.encode(request.password()), Collections.emptyList()),
				RecoveryCodeUtils.hash(recoveryCode));
		log.info("User {} registered.", request.userName());
		return new RecoveryCodeResponseRecord(recoveryCode);
	}

	@Override
	@Transactional
	public void changePassword(String userName, ChangePasswordRequestRecord request) {
		UserEntity user = loadVerified(userName, request.currentPassword());
		passwordPolicy.validate(request.newPassword());
		if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
			throw CustomException.builder().errorCode(ErrorCode.PASSWORD_UNCHANGED).build();
		}

		setPassword(user, request.newPassword());
		refreshTokenService.revokeAllSessions(user.getId());
		log.info("User {} changed their password. All sessions revoked.", userName);
	}

	@Override
	@Transactional
	public void deactivate(String userName, String password) {
		UserEntity user = loadVerified(userName, password);

		user.setEnabled(false);
		user.setUpdatedAt(LocalDateTime.now());
		userRepository.save(user);
		refreshTokenService.revokeAllSessions(user.getId());
		log.info("User {} deactivated. All sessions revoked.", userName);
	}

	@Override
	@Transactional
	public RecoveryCodeResponseRecord regenerateRecoveryCode(String userName, String password) {
		UserEntity user = loadVerified(userName, password);

		String recoveryCode = issueRecoveryCode(user);
		log.info("New recovery code issued for user {}.", userName);
		return new RecoveryCodeResponseRecord(recoveryCode);
	}

	@Override
	@Transactional
	public RecoveryCodeResponseRecord resetPassword(PasswordResetRequestRecord request) {
		passwordPolicy.validate(request.newPassword());

		UserEntity user = userRepository.findByUsernameForUpdate(request.userName()).orElse(null);
		// matches() does the same work whether or not there is a stored hash.
		boolean codeMatches = RecoveryCodeUtils.matches(request.recoveryCode(),
				user == null ? null : user.getRecoveryCodeHash());
		if (user == null || !codeMatches || !user.isEnabled()) {
			log.info("Password reset rejected for user {}.", request.userName());
			throw CustomException.builder().errorCode(ErrorCode.INVALID_CREDENTIALS).build();
		}

		setPassword(user, request.newPassword());
		String newRecoveryCode = issueRecoveryCode(user);
		refreshTokenService.revokeAllSessions(user.getId());
		log.info("Password of user {} reset with a recovery code. All sessions revoked.", request.userName());
		return new RecoveryCodeResponseRecord(newRecoveryCode);
	}

	/** Row-locks the user and checks the password, so concurrent changes to one account run one at a time. */
	private UserEntity loadVerified(String userName, String password) {
		UserEntity user = userRepository.findByUsernameForUpdate(userName)
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.USER_NOT_FOUND).build());
		if (!passwordEncoder.matches(password, user.getPassword())) {
			log.info("Wrong password supplied for a sensitive action by user {}.", userName);
			throw CustomException.builder().errorCode(ErrorCode.INVALID_CREDENTIALS).build();
		}
		return user;
	}

	private void setPassword(UserEntity user, String rawPassword) {
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setUpdatedAt(LocalDateTime.now());
	}

	/** Stores the hash of a new recovery code on the user and returns the code itself. */
	private String issueRecoveryCode(UserEntity user) {
		String recoveryCode = RecoveryCodeUtils.generate();
		user.setRecoveryCodeHash(RecoveryCodeUtils.hash(recoveryCode));
		user.setUpdatedAt(LocalDateTime.now());
		return recoveryCode;
	}

	@Override
	public void getUser(String userName) {
		if (StringUtils.isBlank(userName)) {
			log.info("Invalid user name provided.");
			throw new IllegalArgumentException("Invalid user name provided.");
		}
		log.debug("Fetching details for user name {}.", userName);
		UserEntity userEntity = userRepository.findByUsername(userName)
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.USER_NOT_FOUND).build());

	}

	@Override
	public void getUserById(int userId) {
		// TODO Auto-generated method stub

	}

}
