package com.practice.url_shortner.utility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * One-time password recovery codes: 120 random bits shown as six groups of four
 * Base32 characters ({@code ABCD-EFGH-...}). Only the SHA-256 of the normalized
 * code is stored; the code carries enough entropy that a slow hash is not needed.
 */
public final class RecoveryCodeUtils {

	private static final int CODE_BYTES = 15;
	private static final int GROUP_SIZE = 4;
	private static final SecureRandom RANDOM = new SecureRandom();

	/** Compared against when the user has no stored hash, so the check takes the same time either way. */
	private static final String DUMMY_HASH = DigestUtils.sha256Hex("no-recovery-code");

	private RecoveryCodeUtils() {
	}

	/** A new random code in its display form, e.g. {@code ABCD-EFGH-IJKL-MNOP-QRST-UVWX}. */
	public static String generate() {
		byte[] bytes = new byte[CODE_BYTES];
		RANDOM.nextBytes(bytes);
		// 15 bytes are exactly 24 Base32 characters, so there is no padding to strip.
		String encoded = new Base32().encodeToString(bytes);
		StringBuilder code = new StringBuilder();
		for (int i = 0; i < encoded.length(); i += GROUP_SIZE) {
			if (i > 0) {
				code.append('-');
			}
			code.append(encoded, i, i + GROUP_SIZE);
		}
		return code.toString();
	}

	/** Hex SHA-256 of the code with dashes and whitespace removed and letters upper-cased. */
	public static String hash(String code) {
		return DigestUtils.sha256Hex(normalize(code));
	}

	/** Constant-time check of a presented code against a stored hash; {@code false} if there is no stored hash. */
	public static boolean matches(String presentedCode, String storedHash) {
		String expected = storedHash != null ? storedHash : DUMMY_HASH;
		boolean equal = MessageDigest.isEqual(hash(presentedCode).getBytes(StandardCharsets.UTF_8),
				expected.getBytes(StandardCharsets.UTF_8));
		return storedHash != null && equal;
	}

	private static String normalize(String code) {
		return code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
	}
}
