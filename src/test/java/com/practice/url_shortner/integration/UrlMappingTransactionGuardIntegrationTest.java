package com.practice.url_shortner.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.practice.url_shortner.service.url_mapping.IUrlMappingService;

/**
 * {@code createNewMapping} retries on short code collisions, which cannot work inside an outer transaction (a failed
 * insert marks it rollback-only), so it fails fast if one is active. The guard is a programmatic check rather than
 * {@code @Transactional(propagation = NEVER)}: NEVER keeps each repository call's connection open for the whole method,
 * which starved the connection pool under concurrent requests.
 */
@SpringBootTest
@ActiveProfiles("test")
class UrlMappingTransactionGuardIntegrationTest {

	@Autowired
	private IUrlMappingService urlMappingService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void createNewMapping_throws_whenCalledInsideAnActiveTransaction() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		assertThatThrownBy(
				() -> transaction.executeWithoutResult(status -> urlMappingService.createNewMapping("https://example.org/foo")))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("must not be called inside a transaction");
	}
}
