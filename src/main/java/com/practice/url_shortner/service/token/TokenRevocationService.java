package com.practice.url_shortner.service.token;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.practice.url_shortner.entity.RevokedTokenEntity;
import com.practice.url_shortner.repository.IRevokedTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService implements ITokenRevocationService {

	private final IRevokedTokenRepository revokedTokenRepository;

	@Override
	public void revoke(DecodedJWT token) {
		String jti = token.getId();
		if (StringUtils.isBlank(jti)) {
			log.info("Token has no jti, so it cannot be revoked. It will expire on its own.");
			return;
		}
		if (revokedTokenRepository.existsById(jti)) {
			return;
		}
		try {
			revokedTokenRepository.save(new RevokedTokenEntity(jti, token.getExpiresAtAsInstant()));
			log.debug("Token {} revoked.", jti);
		} catch (DataIntegrityViolationException e) {
			// A concurrent logout with the same token inserted it first.
			log.debug("Token {} was already revoked.", jti);
		}
	}

	@Override
	public boolean isRevoked(String jti) {
		return revokedTokenRepository.existsById(jti);
	}

	@Override
	@Transactional
	@Scheduled(fixedDelayString = "${app.security.revocation-cleanup-interval:PT15M}")
	public int purgeExpired() {
		int deleted = revokedTokenRepository.deleteExpiredBefore(Instant.now());
		if (deleted > 0) {
			log.info("Purged {} expired entries from the token denylist.", deleted);
		}
		return deleted;
	}
}
