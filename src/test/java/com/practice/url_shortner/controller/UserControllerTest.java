package com.practice.url_shortner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.configuration.AppConfiguration;
import com.practice.url_shortner.security.JwtAuthenticationFilter;
import com.practice.url_shortner.security.RateLimitingFilter;
import com.practice.url_shortner.security.SecurityConfig;
import com.practice.url_shortner.service.user.IUserService;
import com.practice.url_shortner.utility.JwtUtils;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, RateLimitingFilter.class, AppConfiguration.class })
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IUserService userService;

	@MockitoBean
	private UserDetailsManager userDetailsManager;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private JwtUtils jwtUtils;

	@Test
	void login_returns200WithTokenBody_onSuccessfulAuthentication() throws Exception {
		when(userService.login(any())).thenReturn(new AuthenticationResponseRecord("a-token", null));

		mockMvc.perform(post("/api/1/login").contentType("application/json")
				.content("{\"userName\":\"test-user\",\"password\":\"test-password\"}")).andExpect(status().isOk())
				.andExpect(content().json("{\"token\":\"a-token\"}"));
	}

	@Test
	void login_returns401_whenAuthenticationManagerThrowsBadCredentials() throws Exception {
		when(userService.login(any())).thenThrow(new BadCredentialsException("bad creds"));

		mockMvc.perform(post("/api/1/login").contentType("application/json")
				.content("{\"userName\":\"test-user\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().json("{\"errorCode\":\"" + ErrorCode.INVALID_CREDENTIALS.getErrorCode() + "\"}"));
	}

	@Test
	void createUser_returns204_onSuccess() throws Exception {
		when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");

		mockMvc.perform(post("/api/1/user").contentType("application/json")
				.content("{\"userName\":\"test-user\",\"password\":\"test-password\"}"))
				.andExpect(status().isNoContent());

		verify(userDetailsManager).createUser(org.mockito.ArgumentMatchers.argThat(userDetails -> userDetails instanceof User
				&& ((User) userDetails).getUsername().equals("test-user")
				&& ((User) userDetails).getPassword().equals("encoded-password")));
	}

	@Test
	void createUser_returns400_whenUsernameAlreadyTaken() throws Exception {
		when(passwordEncoder.encode(any())).thenReturn("encoded-password");
		org.mockito.Mockito.doThrow(CustomException.builder().errorCode(ErrorCode.USER_NAME_NOT_AVAILABLE).build())
				.when(userDetailsManager).createUser(any());

		mockMvc.perform(post("/api/1/user").contentType("application/json")
				.content("{\"userName\":\"test-user\",\"password\":\"test-password\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content()
						.json("{\"errorCode\":\"" + ErrorCode.USER_NAME_NOT_AVAILABLE.getErrorCode() + "\"}"));
	}
}
