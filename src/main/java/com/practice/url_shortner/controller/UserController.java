package com.practice.url_shortner.controller;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.model.CreateUserRequest;
import com.practice.url_shortner.service.user.IUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = { "/api/1" }, version = "1")
public class UserController {

	private final UserDetailsManager userDetailsManager;
	private final PasswordEncoder passwordEncoder;
	private final IUserService userService;

	@PostMapping("/login")
	public AuthenticationResponseRecord login(@RequestBody AuthenticationRequestRecord credentials) {
		return userService.login(credentials);
	}

	@PostMapping("/user")
	public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest request) {
		userDetailsManager.createUser(
				new User(request.userName(), passwordEncoder.encode(request.password()), Collections.emptyList()));

		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}
}
