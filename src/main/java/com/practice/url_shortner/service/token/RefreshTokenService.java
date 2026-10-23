package com.practice.url_shortner.service.token;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.practice.url_shortner.configuration.RefreshTokenProperties;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.RefreshTokenEntity;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.repository.IRefreshTokenRepository;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.utility.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {

	private static final int TOKEN_BYTES = 32;

	/** Covers the gap between computing the access token's expiry and JwtUtils signing it. */
	private static final long ACCESS_EXPIRY_BUFFER_SECONDS = 60;

	/** 256 random bits from a SecureRandom, as URL-safe Base64 without padding. */
	private static final StringKeyGenerator TOKEN_GENERATOR = new Base64StringKeyGenerator(
			Base64.getUrlEncoder().withoutPadding(), TOKEN_BYTES);

	private final IRefreshTokenRepository refreshTokenRepository;
	private final IUserRepository userRepository;
	private final ITokenRevocationService tokenRevocationService;
	private final RefreshTokenProperties properties;
	private final JwtUtils jwtUtils;

	@Override
	@Transactional
	public IssuedSession startSession(String userName) {
		// Serializes concurrent logins of the same user so they cannot all slip under the cap.
		UserEntity user = userRepository.findByUsernameForUpdate(userName)
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.USER_NOT_FOUND).build());
		if (!user.isEnabled()) {
			// Login authenticated the user before this lock; deactivation may have committed in between.
			log.warn("Not starting a session for disabled user {}.", userName);
			throw CustomException.builder().errorCode(ErrorCode.INVALID_CREDENTIALS).build();
		}

		Instant now = Instant.now();
		List<RefreshTokenEntity> active = refreshTokenRepository.findActiveSessions(user.getId(), now);
		int toEvict = active.size() - properties.maxSessionsPerUser() + 1;
		for (int i = 0; i < toEvict; i++) {
			log.info("Evicting oldest session {} of user {}: session limit {} reached.",
					active.get(i).getFamilyId(), userName, properties.maxSessionsPerUser());
			revokeFamily(active.get(i).getFamilyId(), now);
		}

		return issue(user.getId(), userName, UUID.randomUUID().toString(), now.plus(properties.refreshTokenValidity()),
				now);
	}

	@Override
	@Transactional(noRollbackFor = CustomException.class)
	public IssuedSession rotate(String refreshToken) {
		Instant now = Instant.now();
		RefreshTokenEntity current = refreshTokenRepository.findByTokenHash(hash(refreshToken))
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.INVALID_REFRESH_TOKEN).build());

		if (current.isUsed()) {
			// A spent token coming back means it was copied; kill the session for everyone holding it.
			log.warn("Refresh token reuse detected for session {}. Revoking the session.", current.getFamilyId());
			revokeFamily(current.getFamilyId(), now);
			throw CustomException.builder().errorCode(ErrorCode.INVALID_REFRESH_TOKEN).build();
		}
		if (current.isRevoked()) {
			throw CustomException.builder().errorCode(ErrorCode.INVALID_REFRESH_TOKEN).build();
		}
		if (!current.getExpiresAt().isAfter(now)) {
			throw CustomException.builder().errorCode(ErrorCode.SESSION_EXPIRED).build();
		}

		UserEntity user = userRepository.findById(current.getUserId())
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.INVALID_REFRESH_TOKEN).build());

		if (!user.isEnabled()) {
			// Deactivation revokes the user's sessions; this covers any session it missed.
			log.warn("Refresh rejected for disabled user {}. Revoking the session.", user.getUsername());
			revokeFamily(current.getFamilyId(), now);
			throw CustomException.builder().errorCode(ErrorCode.INVALID_REFRESH_TOKEN).build();
		}

		current.setUsed(true);
		refreshTokenRepository.save(current);
		// The new link inherits the absolute expiry, so rotating never extends the session.
		return issue(user.getId(), user.getUsername(), current.getFamilyId(), current.getExpiresAt(), now);
	}

	@Override
	@Transactional
	public void revokeSessionByAccessJti(String jti) {
		refreshTokenRepository.findFirstByCurrentAccessJti(jti).ifPresent(token -> {
			revokeFamily(token.getFamilyId(), Instant.now());
			log.debug("Session {} revoked.", token.getFamilyId());
		});
	}

	@Override
	@Transactional
	public void revokeAllSessions(int userId) {
		Instant now = Instant.now();
		// Not just active sessions: one past its absolute expiry can still hold an access token that works.
		List<String> families = refreshTokenRepository.findFamilyIdsWithLiveCredentials(userId, now);
		for (String familyId : families) {
			revokeFamily(familyId, now);
		}
		log.info("Revoked {} session(s) of user id {}.", families.size(), userId);
	}

	@Override
	@Transactional
	@Scheduled(fixedDelayString = "${app.security.revocation-cleanup-interval:PT15M}", initialDelayString = "${app.security.revocation-cleanup-interval:PT15M}")
	public int purgeExpired() {
		int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now());
		if (deleted > 0) {
			log.info("Purged {} expired refresh tokens.", deleted);
		}
		return deleted;
	}

	private IssuedSession issue(int userId, String userName, String familyId, Instant sessionExpiresAt, Instant now) {
		String rawToken = TOKEN_GENERATOR.generateKey();
		String accessJti = UUID.randomUUID().toString();

		RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.setFamilyId(familyId);
		entity.setUserId(userId);
		entity.setTokenHash(hash(rawToken));
		entity.setExpiresAt(sessionExpiresAt);
		entity.setCurrentAccessJti(accessJti);
		entity.setAccessExpiresAt(
				now.plus(jwtUtils.accessTokenValidity()).plusSeconds(ACCESS_EXPIRY_BUFFER_SECONDS));
		entity.setCreatedAt(now);
		refreshTokenRepository.save(entity);

		return new IssuedSession(userName, rawToken, accessJti);
	}

	/**
	 * Marks every link of the session revoked and denylists any access token of it
	 * that has not expired yet, so the session dies immediately rather than when
	 * its last access token runs out.
	 */
	private void revokeFamily(String familyId, Instant now) {
		for (RefreshTokenEntity token : refreshTokenRepository.findByFamilyId(familyId)) {
			token.setRevoked(true);
			if (token.getAccessExpiresAt().isAfter(now)) {
				tokenRevocationService.revoke(token.getCurrentAccessJti(), token.getAccessExpiresAt());
			}
		}
	}

	private static String hash(String token) {
		return DigestUtils.sha256Hex(token);
	}
}
