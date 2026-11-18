package com.practice.url_shortner.service.url_mapping;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

	private static final int MAX_SHORT_CODE_ATTEMPTS = 5;

	private final IUrlMappingRepository urlMappingRepository;

	private final ShortCodeGenerator shortCodeGenerator;

	private final UserDetailsManager userDetailsManager;

	private final IUserEntityToUserResponseRecordMapper userEntityToUserResponseRecordMapper;

	private final IUserRepository userRepository;

	@Override
	// NEVER: fails fast if a caller wraps this in a transaction, which would break the short code retry loop
	// (see saveWithUniqueShortCode). Must stay on this public method: Spring applies it through the proxy, so it
	// would have no effect on the private method, nor on a call from within this class.
	// Deliberately NOT @Transactional(propagation = NEVER): that opens a synchronization scope which keeps every
	// repository call's EntityManager (and its JDBC connection) open until this method returns, so each request ends
	// up holding several connections at once and concurrent requests starve the pool and deadlock. See
	// saveWithUniqueShortCode for why a surrounding transaction is still unacceptable.
	public NewUrlMappingResponseRecord createNewMapping(String longUrl) {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("createNewMapping must not be called inside a transaction: its short code "
					+ "retry loop cannot recover from a failed insert in an outer transaction.");
		}
		log.info("Request received to create new url mapping for {}", longUrl);

		UrlMappingEntity newUrlMapping = new UrlMappingEntity();
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
		UrlMappingEntity savedEntity = saveWithUniqueShortCode(newUrlMapping);

		log.info("New url mapping created for long url {} with short code {}.", longUrl, savedEntity.getShortCode());

		return new NewUrlMappingResponseRecord(savedEntity.getShortCode(),
				"http://localhost:8080/" + savedEntity.getShortCode());
	}

	/**
	 * Saves the mapping under a freshly generated short code, regenerating and retrying when the code collides with an
	 * existing one.
	 * <p>
	 * Uniqueness is decided by the database: {@code url_mapping.short_code} has a unique index, so we insert
	 * optimistically and treat a constraint violation as a collision, rather than doing check-then-insert (which races
	 * under concurrency and costs an extra query on every request for a collision that is very rare).
	 * <p>
	 * <b>This must not run inside an outer transaction.</b> A failed insert marks the surrounding transaction
	 * rollback-only, so retrying within it would not work: the next {@code save} (or the commit) would fail even though
	 * the retry used a fresh code. That is why {@link #createNewMapping} throws if a transaction is active, and each
	 * {@code save} runs in its own transaction. If this ever needs to join a larger transaction, move the retry loop
	 * outside of it or run each attempt in {@code REQUIRES_NEW}.
	 * <p>
	 * A {@link DataIntegrityViolationException} is not necessarily a short code collision (other constraints on the
	 * table could trip it too), and the exception text for the violated index differs between MySQL and H2. So instead
	 * of parsing it, we confirm with {@code existsByShortCode}: if the code is not taken, the failure had another cause
	 * and is rethrown instead of being retried.
	 */
	private UrlMappingEntity saveWithUniqueShortCode(UrlMappingEntity mapping) {
		for (int attempt = 1; attempt <= MAX_SHORT_CODE_ATTEMPTS; attempt++) {
			String candidate = shortCodeGenerator.generate();
			mapping.setShortCode(candidate);
			try {
				return urlMappingRepository.save(mapping);
			} catch (DataIntegrityViolationException e) {
				if (!urlMappingRepository.existsByShortCode(candidate)) {
					throw e;
				}
				log.warn("Short code collision on attempt {}/{} for code {}, retrying with a new code.", attempt,
						MAX_SHORT_CODE_ATTEMPTS, candidate);
			}
		}
		// With ~2.2e14 possible codes, exhausting the retries means something is wrong (e.g. a broken generator).
		log.error("Could not generate a unique short code after {} attempts.", MAX_SHORT_CODE_ATTEMPTS);
		throw CustomException.builder().errorCode(ErrorCode.SHORT_CODE_GENERATION_FAILED).build();
	}

	@Override
	public void disableMapping(String longUrl) {
		// TODO Auto-generated method stub

	}

	@Override
	public String resolveShortCodeToUrl(String shortCode) {
		UrlMappingEntity urlMappingEntity = urlMappingRepository.findActiveByShortCode(shortCode)
				.orElseThrow(() -> CustomException.builder().errorCode(ErrorCode.NOT_FOUND).build());
		return urlMappingEntity.getLongUrl();
	}
}
