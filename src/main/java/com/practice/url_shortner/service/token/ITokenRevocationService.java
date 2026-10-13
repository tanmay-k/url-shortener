package com.practice.url_shortner.service.token;

import java.time.Instant;

import com.auth0.jwt.interfaces.DecodedJWT;

public interface ITokenRevocationService {

	/**
	 * Denylists the token (which must carry a {@code jti}) until it would have
	 * expired anyway. Revoking the same token twice is harmless.
	 */
	void revoke(DecodedJWT token);

	/** Denylists the {@code jti} until {@code expiresAt}, after which the row can be purged. */
	void revoke(String jti, Instant expiresAt);

	boolean isRevoked(String jti);

	/** Removes denylist rows for tokens that have expired and can no longer be used. */
	int purgeExpired();
}
