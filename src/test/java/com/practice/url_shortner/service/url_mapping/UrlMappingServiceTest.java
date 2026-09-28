package com.practice.url_shortner.service.url_mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.UserDetailsManager;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UrlMappingEntity;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.mapper.IUserEntityToUserResponseRecordMapper;
import com.practice.url_shortner.model.NewUrlMappingResponseRecord;
import com.practice.url_shortner.repository.IUrlMappingRepository;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.testsupport.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class UrlMappingServiceTest {

	@Mock
	private IUrlMappingRepository urlMappingRepository;

	@Mock
	private UserDetailsManager userDetailsManager;

	@Mock
	private IUserEntityToUserResponseRecordMapper userEntityToUserResponseRecordMapper;

	@Mock
	private IUserRepository userRepository;

	private UrlMappingService urlMappingService;

	private MockedStatic<SecurityContextHolder> securityContextHolderMock;

	@BeforeEach
	void setUp() {
		urlMappingService = new UrlMappingService(urlMappingRepository, userDetailsManager,
				userEntityToUserResponseRecordMapper, userRepository);
	}

	@AfterEach
	void tearDown() {
		if (securityContextHolderMock != null) {
			securityContextHolderMock.close();
		}
	}

	private void mockAuthenticatedPrincipal(String username) {
		SecurityContext securityContext = Mockito.mock(SecurityContext.class);
		Authentication authentication = Mockito.mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(username);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
		securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
	}

	@Test
	void createNewMapping_savesEntityAndReturnsResponseWithCorrectShortCodeLengthAndUrl() {
		mockAuthenticatedPrincipal("test-user");
		UserEntity userEntity = TestDataFactory.aUserEntity(u -> u.setId(42));
		when(userRepository.findByUsername("test-user")).thenReturn(Optional.of(userEntity));
		when(urlMappingRepository.save(any(UrlMappingEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		NewUrlMappingResponseRecord response = urlMappingService.createNewMapping("https://example.org/foo");

		assertThat(response.shortCode()).hasSize(8);
		assertThat(response.shortCode()).matches("[a-zA-Z0-9]{8}");
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/" + response.shortCode());
	}

	@Test
	void createNewMapping_setsCreatedByAndUpdatedByToCurrentUserId() {
		mockAuthenticatedPrincipal("test-user");
		UserEntity userEntity = TestDataFactory.aUserEntity(u -> u.setId(42));
		when(userRepository.findByUsername("test-user")).thenReturn(Optional.of(userEntity));
		when(urlMappingRepository.save(any(UrlMappingEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		urlMappingService.createNewMapping("https://example.org/foo");

		ArgumentCaptor<UrlMappingEntity> captor = ArgumentCaptor.forClass(UrlMappingEntity.class);
		verify(urlMappingRepository).save(captor.capture());
		UrlMappingEntity saved = captor.getValue();
		assertThat(saved.getCreatedBy()).isEqualTo(42);
		assertThat(saved.getUpdatedBy()).isEqualTo(42);
		assertThat(saved.isEnabled()).isTrue();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(saved.getLongUrl()).isEqualTo("https://example.org/foo");
	}

	@Test
	void createNewMapping_throwsCustomExceptionWithUserNotFound_whenPrincipalUserMissing() {
		mockAuthenticatedPrincipal("ghost-user");
		when(userRepository.findByUsername("ghost-user")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> urlMappingService.createNewMapping("https://example.org/foo"))
				.isInstanceOf(CustomException.class)
				.extracting(ex -> ((CustomException) ex).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void resolveShortCodeToUrl_returnsLongUrl_whenEnabledMappingExists() {
		UrlMappingEntity entity = TestDataFactory.aUrlMappingEntity(e -> e.setLongUrl("https://example.org/bar"));
		when(urlMappingRepository.findByShortCodeAndEnabled("abc12345", true)).thenReturn(Optional.of(entity));

		String longUrl = urlMappingService.resolveShortCodeToUrl("abc12345");

		assertThat(longUrl).isEqualTo("https://example.org/bar");
	}

	@Test
	void resolveShortCodeToUrl_throwsCustomExceptionWithNotFound_whenMappingMissingOrDisabled() {
		when(urlMappingRepository.findByShortCodeAndEnabled("missing", true)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> urlMappingService.resolveShortCodeToUrl("missing"))
				.isInstanceOf(CustomException.class)
				.extracting(ex -> ((CustomException) ex).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	void disableMapping_currentlyNoOp() {
		urlMappingService.disableMapping("https://example.org/foo");

		verifyNoInteractions(urlMappingRepository, userRepository, userDetailsManager);
	}
}
