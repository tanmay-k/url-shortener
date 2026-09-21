package com.practice.url_shortner.service.user;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.utility.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

	private final AuthenticationManager authenticationManager;

	private final JwtUtils jwtUtils;

	private IUserRepository userRepository;

	@Override
	public AuthenticationResponseRecord login(AuthenticationRequestRecord credentials) {
		log.debug("Request received to authenticate user: {}", credentials.userName());
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(credentials.userName(), credentials.password()));
		log.debug("Authentication successful for user {}", credentials.userName());
		String token = jwtUtils.createToken(credentials.userName());
		log.info("New token issued to user {}. The token is {}", credentials.userName(), token);
		return new AuthenticationResponseRecord(token, null);
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
