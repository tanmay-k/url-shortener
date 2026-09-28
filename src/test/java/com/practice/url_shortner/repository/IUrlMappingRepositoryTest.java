package com.practice.url_shortner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.practice.url_shortner.entity.UrlMappingEntity;
import com.practice.url_shortner.testsupport.TestDataFactory;

@DataJpaTest
@ActiveProfiles("test")
class IUrlMappingRepositoryTest {

	@Autowired
	private IUrlMappingRepository urlMappingRepository;

	@Test
	void findByShortCode_returnsMapping_whenExists() {
		urlMappingRepository.save(TestDataFactory.aUrlMappingEntity(m -> m.setShortCode("findme1")));

		Optional<UrlMappingEntity> found = urlMappingRepository.findByShortCode("findme1");

		assertThat(found).isPresent();
		assertThat(found.get().getShortCode()).isEqualTo("findme1");
	}

	@Test
	void findByShortCode_returnsEmpty_whenNotExists() {
		Optional<UrlMappingEntity> found = urlMappingRepository.findByShortCode("missing1");

		assertThat(found).isEmpty();
	}

	@Test
	void findByShortCodeAndEnabled_returnsMapping_onlyWhenEnabledMatches() {
		urlMappingRepository.save(TestDataFactory.aUrlMappingEntity(m -> {
			m.setShortCode("enabled1");
			m.setEnabled(true);
		}));
		urlMappingRepository.save(TestDataFactory.aUrlMappingEntity(m -> {
			m.setShortCode("disabld1");
			m.setEnabled(false);
		}));

		assertThat(urlMappingRepository.findByShortCodeAndEnabled("enabled1", true)).isPresent();
		assertThat(urlMappingRepository.findByShortCodeAndEnabled("disabld1", true)).isEmpty();
	}
}
