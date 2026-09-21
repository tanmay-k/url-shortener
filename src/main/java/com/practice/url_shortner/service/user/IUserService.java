package com.practice.url_shortner.service.user;

import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;

public interface IUserService {

	AuthenticationResponseRecord login(AuthenticationRequestRecord credentials);

	void getUser(String userName);

	void getUserById(int userId);
}
