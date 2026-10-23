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

	@Autowired
	private IUserRepository userRepository;

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

	private int savedUser(String userName, boolean enabled) {
		return userRepository.save(TestDataFactory.aUserEntity(u -> {
			u.setUsername(userName);
			u.setEnabled(enabled);
		})).getId();
	}

	private void savedMapping(String shortCode, boolean enabled, int ownerId) {
		urlMappingRepository.save(TestDataFactory.aUrlMappingEntity(m -> {
			m.setShortCode(shortCode);
			m.setEnabled(enabled);
			m.setCreatedBy(ownerId);
		}));
	}

	@Test
	void findActiveByShortCode_returnsMapping_onlyWhenMappingIsEnabled() {
		int owner = savedUser("owner-1", true);
		savedMapping("enabled1", true, owner);
		savedMapping("disabld1", false, owner);

		assertThat(urlMappingRepository.findActiveByShortCode("enabled1")).isPresent();
		assertThat(urlMappingRepository.findActiveByShortCode("disabld1")).isEmpty();
	}

	@Test
	void findActiveByShortCode_returnsEmpty_whenTheOwnerIsDisabled() {
		int owner = savedUser("owner-2", false);
		savedMapping("owned001", true, owner);

		assertThat(urlMappingRepository.findActiveByShortCode("owned001")).isEmpty();
	}

	@Test
	void findActiveByShortCode_resolvesAgain_whenTheOwnerIsEnabledAgain() {
		int owner = savedUser("owner-3", false);
		savedMapping("owned002", true, owner);
		userRepository.findById(owner).orElseThrow().setEnabled(true);
		userRepository.flush();

		assertThat(urlMappingRepository.findActiveByShortCode("owned002")).isPresent();
	}

	@Test
	void findActiveByShortCode_returnsEmpty_whenTheMappingHasNoKnownOwner() {
		savedMapping("orphan01", true, 9999);

		assertThat(urlMappingRepository.findActiveByShortCode("orphan01")).isEmpty();
	}
}
