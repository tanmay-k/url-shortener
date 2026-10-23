package com.practice.url_shortner.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecoveryCodeUtilsTest {

	@Test
	void generate_producesSixGroupsOfFourBase32Characters() {
		String code = RecoveryCodeUtils.generate();

		assertThat(code).matches("([A-Z2-7]{4}-){5}[A-Z2-7]{4}");
	}

	@Test
	void generate_producesADifferentCodeEachTime() {
		assertThat(RecoveryCodeUtils.generate()).isNotEqualTo(RecoveryCodeUtils.generate());
	}

	@Test
	void hash_isAHexSha256_thatDoesNotContainTheCode() {
		String code = RecoveryCodeUtils.generate();

		String hash = RecoveryCodeUtils.hash(code);

		assertThat(hash).matches("[0-9a-f]{64}").doesNotContain(code.replace("-", ""));
	}

	@Test
	void hash_ignoresCaseDashesAndWhitespace() {
		assertThat(RecoveryCodeUtils.hash("abcd-efgh-ijkl-mnop-qrst-uvwx"))
				.isEqualTo(RecoveryCodeUtils.hash("ABCDEFGHIJKLMNOPQRSTUVWX"))
				.isEqualTo(RecoveryCodeUtils.hash(" ABCD EFGH-IJKL-MNOP-QRST-UVWX\n"));
	}

	@Test
	void matches_isTrue_forTheCodeTheHashWasMadeFrom() {
		String code = RecoveryCodeUtils.generate();

		assertThat(RecoveryCodeUtils.matches(code, RecoveryCodeUtils.hash(code))).isTrue();
	}

	@Test
	void matches_isFalse_forAnotherCode() {
		String hash = RecoveryCodeUtils.hash(RecoveryCodeUtils.generate());

		assertThat(RecoveryCodeUtils.matches(RecoveryCodeUtils.generate(), hash)).isFalse();
	}

	@Test
	void matches_isFalse_whenThereIsNoStoredHash() {
		assertThat(RecoveryCodeUtils.matches(RecoveryCodeUtils.generate(), null)).isFalse();
	}
}
