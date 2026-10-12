package com.practice.url_shortner.service.token;

import com.auth0.jwt.interfaces.DecodedJWT;

public interface ITokenRevocationService {

	/**
	 * Denylists the token until it would have expired anyway. Revoking a token that
	 * has no {@code jti} (issued before revocation support) is a no-op, and
	 * revoking the same token twice is harmless.
	 */
	void revoke(DecodedJWT token);

	boolean isRevoked(String jti);

	/** Removes denylist rows for tokens that have expired and can no longer be used. */
	int purgeExpired();
}
