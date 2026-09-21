package com.practice.url_shortner.exception;

import java.time.Instant;

import com.practice.url_shortner.constants.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class CustomException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private ErrorCode errorCode;

	private final Instant timestamp = Instant.now();
}
