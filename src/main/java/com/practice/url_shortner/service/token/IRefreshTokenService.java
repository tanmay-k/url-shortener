package com.practice.url_shortner.service.token;

public interface IRefreshTokenService {

	/**
	 * Credentials for a freshly issued session or rotation.
	 *
	 * @param userName     the user the session belongs to
	 * @param refreshToken the raw opaque refresh token; returned to the client once
	 * @param accessJti    the {@code jti} the caller must put on the access token
	 */
	record IssuedSession(String userName, String refreshToken, String accessJti) {}

	/**
	 * Starts a new session for an already authenticated user, evicting the oldest
	 * sessions first if the user is at the configured limit.
	 */
	IssuedSession startSession(String userName);

	/**
	 * Exchanges a refresh token for a new one in the same session. The presented
	 * token is spent; presenting a spent token revokes the whole session.
	 *
	 * @throws com.practice.url_shortner.exception.CustomException if the token is
	 *         unknown, spent, revoked or expired
	 */
	IssuedSession rotate(String refreshToken);

	/**
	 * Ends the session that issued the access token with this {@code jti}. Does
	 * nothing if there is no such session.
	 */
	void revokeSessionByAccessJti(String jti);

	/** Removes sessions whose absolute lifetime has passed. */
	int purgeExpired();
}
