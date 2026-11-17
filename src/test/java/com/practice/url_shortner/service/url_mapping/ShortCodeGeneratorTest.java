package com.practice.url_shortner.service.url_mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

	private final ShortCodeGenerator generator = new ShortCodeGenerator();

	@Test
	void generate_returnsBase62CodeOfConfiguredLength() {
		assertThat(generator.generate()).matches("[a-zA-Z0-9]{" + ShortCodeGenerator.SHORT_CODE_LENGTH + "}");
	}

	@Test
	void generate_returnsDifferentCodesOnSuccessiveCalls() {
		assertThat(generator.generate()).isNotEqualTo(generator.generate());
	}
}
