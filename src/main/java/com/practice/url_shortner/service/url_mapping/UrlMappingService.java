package com.practice.url_shortner.service.url_mapping;

import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UrlMappingEntity;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.mapper.IUserEntityToUserResponseRecordMapper;
import com.practice.url_shortner.model.NewUrlMappingResponseRecord;
import com.practice.url_shortner.repository.IUrlMappingRepository;
import com.practice.url_shortner.repository.IUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlMappingService implements IUrlMappingService {

	private static final int SHORT_CODE_LENGTH = 8;

	private final IUrlMappingRepository urlMappingRepository;

	private final UserDetailsManager userDetailsManager;

	private final IUserEntityToUserResponseRecordMapper userEntityToUserResponseRecordMapper;

	private final IUserRepository userRepository;

	private final ClickEventPublisher clickEventPublisher;

	@Override
	//	@Transactional
	public NewUrlMappingResponseRecord createNewMapping(String longUrl) {
		log.info("Request received to create new url mapping for {}", longUrl);
		String newShortCode = RandomStringUtils.secureStrong().nextAlphanumeric(SHORT_CODE_LENGTH);

		UrlMappingEntity newUrlMapping = new UrlMappingEntity();
		newUrlMapping.setShortCode(newShortCode);
		newUrlMapping.setLongUrl(longUrl);

		LocalDateTime now = LocalDateTime.now();
		newUrlMapping.setCreatedAt(now);
		newUrlMapping.setUpdatedAt(now);

		String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		UserEntity userEntity = userRepository.findByUsername(userName)
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.USER_NOT_FOUND).build());
		//		UserResponse dbUser = userEntityToUserResponseRecordMapper.convert(userEntity);
		newUrlMapping.setCreatedBy(userEntity.getId());
		newUrlMapping.setUpdatedBy(userEntity.getId());
		newUrlMapping.setEnabled(true);
		UrlMappingEntity savedEntity = urlMappingRepository.save(newUrlMapping);

		log.info("New url mapping created for long url {} with short code {}.", longUrl, savedEntity.getShortCode());

		return new NewUrlMappingResponseRecord(newShortCode, "http://localhost:8080/" + newShortCode);
	}

	@Override
	public void disableMapping(String longUrl) {
		// TODO Auto-generated method stub

	}

	private boolean isShortCodeUnique(String shortCode) {
		return true;
	}

	@Override
	public String resolveShortCodeToUrl(String shortCode) {
		return resolveShortCodeToUrl(shortCode, null);
	}

	@Override
	public String resolveShortCodeToUrl(String shortCode, String userAgent) {
		UrlMappingEntity urlMappingEntity = urlMappingRepository.findByShortCodeAndEnabled(shortCode, true)
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.NOT_FOUND).build());
		clickEventPublisher.publishClickEvent(userAgent, shortCode);
		return urlMappingEntity.getLongUrl();
	}
}
