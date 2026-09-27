package com.practice.url_shortner.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.security.JwtAuthenticationFilter;
import com.practice.url_shortner.security.SecurityConfig;
import com.practice.url_shortner.service.url_mapping.IUrlMappingService;
import com.practice.url_shortner.utility.JwtUtils;

@WebMvcTest(RedirectionController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class RedirectionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IUrlMappingService urlMappingService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private JwtUtils jwtUtils;

	@Test
	void sendRedirect_returns301WithLocationHeader_whenMappingExists() throws Exception {
		when(urlMappingService.resolveShortCodeToUrl("abc12345")).thenReturn("https://example.org/target");

		mockMvc.perform(get("/abc12345")).andExpect(status().isMovedPermanently())
				.andExpect(header().string("Location", "https://example.org/target"));
	}

	@Test
	void sendRedirect_returns404_whenMappingNotFoundOrDisabled() throws Exception {
		when(urlMappingService.resolveShortCodeToUrl("missing1"))
				.thenThrow(CustomException.builder().errorCode(ErrorCode.NOT_FOUND).build());

		mockMvc.perform(get("/missing1")).andExpect(status().isNotFound());
	}
}
