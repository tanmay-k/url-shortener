package com.practice.url_shortner.service.user;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.service.token.IRefreshTokenService;
import com.practice.url_shortner.service.token.IRefreshTokenService.IssuedSession;
import com.practice.url_shortner.service.token.ITokenRevocationService;
import com.practice.url_shortner.utility.JwtUtils;

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

	private IUserRepository userRepository;

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
