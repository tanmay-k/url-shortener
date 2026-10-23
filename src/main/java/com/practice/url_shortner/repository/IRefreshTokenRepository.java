package com.practice.url_shortner.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.practice.url_shortner.entity.RefreshTokenEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	/** Locks the row so two concurrent refreshes with the same token cannot both succeed. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	Optional<RefreshTokenEntity> findFirstByCurrentAccessJti(String currentAccessJti);

	List<RefreshTokenEntity> findByFamilyId(String familyId);

	/** The live link of each active session of the user, oldest session first. */
	@Query("""
			SELECT t FROM RefreshTokenEntity t
			WHERE t.userId = :userId AND t.used = false AND t.revoked = false AND t.expiresAt > :now
			ORDER BY t.expiresAt ASC, t.id ASC""")
	List<RefreshTokenEntity> findActiveSessions(int userId, Instant now);

	/**
	 * Sessions of the user that can still be used: a link that is not revoked and either
	 * has an access token that has not expired or is the live refresh token of a session
	 * that has not expired. Unlike {@link #findActiveSessions}, this includes a session whose
	 * refresh expiry has passed but whose last access token is still valid.
	 */
	@Query("""
			SELECT DISTINCT t.familyId FROM RefreshTokenEntity t
			WHERE t.userId = :userId AND t.revoked = false
			AND (t.accessExpiresAt > :now OR (t.used = false AND t.expiresAt > :now))""")
	List<String> findFamilyIdsWithLiveCredentials(int userId, Instant now);

	@Modifying
	@Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :cutoff")
	int deleteExpiredBefore(Instant cutoff);
}
