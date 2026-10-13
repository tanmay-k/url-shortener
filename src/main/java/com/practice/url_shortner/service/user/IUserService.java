package com.practice.url_shortner.service.user;

import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;

public interface IUserService {

	AuthenticationResponseRecord login(AuthenticationRequestRecord credentials);

	/** Revokes the given JWT so it is rejected for the rest of its lifetime. */
	void logout(String token);

	void getUser(String userName);

	void getUserById(int userId);
}
