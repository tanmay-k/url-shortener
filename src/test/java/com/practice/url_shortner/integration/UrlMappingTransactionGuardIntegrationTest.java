package com.practice.url_shortner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.practice.url_shortner.service.url_mapping.IUrlMappingService;

/**
 * {@code createNewMapping} retries on short code collisions, which cannot work inside an outer transaction (a failed
 * insert marks it rollback-only). It is annotated {@code Propagation.NEVER} so misuse fails fast; this needs a real
 * Spring proxy, which the plain unit tests don't have.
 */
@SpringBootTest
@ActiveProfiles("test")
class UrlMappingTransactionGuardIntegrationTest {

	@Autowired
	private IUrlMappingService urlMappingService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void createNewMapping_isCalledThroughASpringProxy() {
		assertThat(AopUtils.isAopProxy(urlMappingService)).isTrue();
	}

	@Test
	void createNewMapping_throws_whenCalledInsideAnActiveTransaction() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		assertThatThrownBy(
				() -> transaction.executeWithoutResult(status -> urlMappingService.createNewMapping("https://example.org/foo")))
				.isInstanceOf(IllegalTransactionStateException.class);
	}
}
