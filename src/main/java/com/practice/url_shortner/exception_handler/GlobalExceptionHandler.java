package com.practice.url_shortner.exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(exception = CustomException.class)
	ResponseEntity<ErrorResponse> handleApplicationException(CustomException exception) {
		log.error("Request failed with exception.", exception);
		return new ResponseEntity<ErrorResponse>(
				ErrorResponse.builder().errorCode(exception.getErrorCode().getErrorCode())
				.message(exception.getErrorCode().getMessage()).build(),
				exception.getErrorCode().getDefaultHttpStatus());
	}

	@ExceptionHandler(exception = Exception.class)
	ResponseEntity<ErrorResponse> handleException(Exception exception){
		log.error("Something went wrong.", exception);
		return new ResponseEntity<ErrorResponse>(
				ErrorResponse.builder().errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode())
				.message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()).build(),
				HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(exception = BadCredentialsException.class)
	ResponseEntity<ErrorResponse> handlerBadCredentialsException(BadCredentialsException badCredsException){
		log.info("User authentication failed");
		return new ResponseEntity<ErrorResponse>(
				ErrorResponse.builder().errorCode(ErrorCode.INVALID_CREDENTIALS.getErrorCode())
						.message(ErrorCode.INVALID_CREDENTIALS.getMessage()).build(),
				ErrorCode.INVALID_CREDENTIALS.getDefaultHttpStatus());
	}
}