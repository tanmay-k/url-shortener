package com.practice.url_shortner.validator;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeUrlValidator implements ConstraintValidator<SafeUrl, String> {

	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (StringUtils.isBlank(value)) {
			return false;
		}
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
				return false;
			}

			// Resolve host
			String host = uri.getHost();
			if (host == null) {
				return false;
			}

			InetAddress inetAddress = InetAddress.getByName(host);

			//			// Block localhost
			//			if (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()) {
			//				return false;
			//			}

			// Block private IP ranges
			if (inetAddress.isSiteLocalAddress()) {
				return false;
			}

			return true;
		} catch (URISyntaxException | UnknownHostException e) {
			return false;
		}
	}
}
