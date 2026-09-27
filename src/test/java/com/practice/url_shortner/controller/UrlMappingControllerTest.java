package com.practice.url_shortner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.practice.url_shortner.model.NewUrlMappingResponseRecord;
import com.practice.url_shortner.security.JwtAuthenticationFilter;
import com.practice.url_shortner.security.SecurityConfig;
import com.practice.url_shortner.service.url_mapping.IUrlMappingService;
import com.practice.url_shortner.utility.JwtUtils;

@WebMvcTest(UrlMappingController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class UrlMappingControllerTest {

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
	@WithMockUser
	void createNewShortUrl_returns200WithMappingResponse_whenAuthenticatedAndUrlValid() throws Exception {
		when(urlMappingService.createNewMapping(any()))
				.thenReturn(new NewUrlMappingResponseRecord("abc12345", "http://localhost:8080/abc12345"));

		mockMvc.perform(post("/api/1/short-url").contentType("application/json")
				.content("{\"longUrl\":\"https://93.184.216.34/\"}")).andExpect(status().isOk())
				.andExpect(content().json("{\"shortCode\":\"abc12345\"}"));
	}

	@Test
	void createNewShortUrl_returns403_whenUnauthenticated() throws Exception {
		// Spring Security's default AuthenticationEntryPoint (no explicit one
		// configured in SecurityConfig) responds with 403, not 401, for
		// unauthenticated requests to a protected endpoint.
		mockMvc.perform(post("/api/1/short-url").contentType("application/json")
				.content("{\"longUrl\":\"https://93.184.216.34/\"}")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser
	void createNewShortUrl_returns500_whenBodyFailsSafeUrlValidation() throws Exception {
		// GlobalExceptionHandler's generic Exception handler catches
		// MethodArgumentNotValidException too (it has no dedicated handler for
		// it), mapping @Valid failures to 500 instead of the usual 400. This
		// pins that current behavior.
		mockMvc.perform(post("/api/1/short-url").contentType("application/json")
				.content("{\"longUrl\":\"http://192.168.1.10/\"}")).andExpect(status().isInternalServerError());
	}
}
