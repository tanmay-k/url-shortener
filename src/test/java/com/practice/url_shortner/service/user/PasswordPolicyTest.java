package com.practice.url_shortner.service.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.practice.url_shortner.configuration.PasswordPolicyProperties;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;

class PasswordPolicyTest {

	private static PasswordPolicy policy(int min, int max, boolean upper, boolean lower, boolean digit,
			boolean special) {
		return new PasswordPolicy(new PasswordPolicyProperties(min, max, upper, lower, digit, special));
	}

	private static void assertWeak(PasswordPolicy policy, String password) {
		assertThatThrownBy(() -> policy.validate(password)).isInstanceOf(CustomException.class)
				.extracting(ex -> ((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.WEAK_PASSWORD);
	}

	@Test
	void validate_acceptsAnyPasswordOfTheMinimumLength_whenNoCharacterClassesRequired() {
		PasswordPolicy policy = policy(8, 72, false, false, false, false);

		assertThatCode(() -> policy.validate("aaaaaaaa")).doesNotThrowAnyException();
	}

	@Test
	void validate_rejectsNullAndTooShortPasswords() {
		PasswordPolicy policy = policy(8, 72, false, false, false, false);

		assertWeak(policy, null);
		assertWeak(policy, "short");
		assertWeak(policy, "1234567");
	}

	@Test
	void validate_rejectsPasswordsLongerThanTheMaximumInBytes() {
		PasswordPolicy policy = policy(1, 10, false, false, false, false);

		assertThatCode(() -> policy.validate("a".repeat(10))).doesNotThrowAnyException();
		assertWeak(policy, "a".repeat(11));
		// Five two-byte characters are 10 bytes; six are 12.
		assertThatCode(() -> policy.validate("é".repeat(5))).doesNotThrowAnyException();
		assertWeak(policy, "é".repeat(6));
	}

	@Test
	void validate_enforcesEachConfiguredCharacterClass() {
		assertWeak(policy(1, 72, true, false, false, false), "lowercase1!");
		assertWeak(policy(1, 72, false, true, false, false), "UPPERCASE1!");
		assertWeak(policy(1, 72, false, false, true, false), "NoDigits!");
		assertWeak(policy(1, 72, false, false, false, true), "NoSpecial1");
	}

	@Test
	void validate_acceptsAPasswordThatMeetsEveryConfiguredRule() {
		PasswordPolicy policy = policy(8, 72, true, true, true, true);

		assertThatCode(() -> policy.validate("Passw0rd!")).doesNotThrowAnyException();
	}

	@Test
	void validate_doesNotEnforceClassesThatAreNotConfigured() {
		PasswordPolicy policy = policy(8, 72, false, false, false, false);

		assertThatCode(() -> policy.validate("        ")).doesNotThrowAnyException();
	}
}
