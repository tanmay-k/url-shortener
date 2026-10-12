package com.practice.url_shortner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.practice.url_shortner.entity.RevokedTokenEntity;

@DataJpaTest
@ActiveProfiles("test")
class IRevokedTokenRepositoryTest {

	@Autowired
	private IRevokedTokenRepository revokedTokenRepository;

	@Test
	void deleteExpiredBefore_removesOnlyExpiredRows() {
		Instant now = Instant.now();
		revokedTokenRepository.save(new RevokedTokenEntity("expired", now.minusSeconds(60)));
		revokedTokenRepository.save(new RevokedTokenEntity("active", now.plusSeconds(60)));

		int deleted = revokedTokenRepository.deleteExpiredBefore(now);

		assertThat(deleted).isEqualTo(1);
		assertThat(revokedTokenRepository.existsById("expired")).isFalse();
		assertThat(revokedTokenRepository.existsById("active")).isTrue();
	}

	@Test
	void deleteExpiredBefore_returnsZero_whenNothingExpired() {
		revokedTokenRepository.save(new RevokedTokenEntity("active", Instant.now().plusSeconds(60)));

		assertThat(revokedTokenRepository.deleteExpiredBefore(Instant.now())).isZero();
	}
}
