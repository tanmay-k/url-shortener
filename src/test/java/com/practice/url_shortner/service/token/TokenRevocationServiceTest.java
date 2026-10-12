package com.practice.url_shortner.service.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.practice.url_shortner.entity.RevokedTokenEntity;
import com.practice.url_shortner.repository.IRevokedTokenRepository;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

	@Mock
	private IRevokedTokenRepository revokedTokenRepository;

	@InjectMocks
	private TokenRevocationService service;

	private DecodedJWT token(String jti, Instant expiresAt) {
		DecodedJWT jwt = mock(DecodedJWT.class);
		when(jwt.getId()).thenReturn(jti);
		lenient().when(jwt.getExpiresAtAsInstant()).thenReturn(expiresAt);
		return jwt;
	}

	@Test
	void revoke_savesJtiWithTokenExpiry() {
		Instant expiry = Instant.now().plusSeconds(600);
		when(revokedTokenRepository.existsById("jti-1")).thenReturn(false);

		service.revoke(token("jti-1", expiry));

		ArgumentCaptor<RevokedTokenEntity> captor = ArgumentCaptor.forClass(RevokedTokenEntity.class);
		verify(revokedTokenRepository).save(captor.capture());
		assertThat(captor.getValue().getJti()).isEqualTo("jti-1");
		assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiry);
	}

	@Test
	void revoke_doesNothing_whenTokenHasNoJti() {
		service.revoke(token(null, null));

		verify(revokedTokenRepository, never()).save(any());
	}

	@Test
	void revoke_doesNotSaveAgain_whenAlreadyRevoked() {
		when(revokedTokenRepository.existsById("jti-1")).thenReturn(true);

		service.revoke(token("jti-1", Instant.now().plusSeconds(600)));

		verify(revokedTokenRepository, never()).save(any());
	}

	@Test
	void revoke_ignoresDuplicateKey_whenConcurrentLogoutInsertedFirst() {
		when(revokedTokenRepository.existsById("jti-1")).thenReturn(false);
		when(revokedTokenRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

		service.revoke(token("jti-1", Instant.now().plusSeconds(600)));
	}

	@Test
	void isRevoked_delegatesToRepository() {
		when(revokedTokenRepository.existsById("jti-1")).thenReturn(true);

		assertThat(service.isRevoked("jti-1")).isTrue();
		assertThat(service.isRevoked("jti-2")).isFalse();
	}

	@Test
	void purgeExpired_returnsNumberOfDeletedRows() {
		when(revokedTokenRepository.deleteExpiredBefore(any())).thenReturn(3);

		assertThat(service.purgeExpired()).isEqualTo(3);
	}
}
