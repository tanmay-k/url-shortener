package com.practice.url_shortner.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ErrorResponse {

	private String errorCode;

	private String message;
}
