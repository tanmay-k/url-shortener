package com.practice.url_shortner.service.user;

import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;

public interface IUserService {

	AuthenticationResponseRecord login(AuthenticationRequestRecord credentials);

	/** Trades a refresh token for a new access token and a new refresh token. The old refresh token is spent. */
	AuthenticationResponseRecord refresh(String refreshToken);

	/** Revokes the given JWT and the session it belongs to. The JWT is rejected for the rest of its lifetime. */
	void logout(String token);

	void getUser(String userName);

	void getUserById(int userId);
}
