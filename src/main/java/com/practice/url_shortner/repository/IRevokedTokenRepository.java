package com.practice.url_shortner.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.practice.url_shortner.entity.RevokedTokenEntity;

@Repository
public interface IRevokedTokenRepository extends JpaRepository<RevokedTokenEntity, String> {

	@Modifying
	@Query("DELETE FROM RevokedTokenEntity t WHERE t.expiresAt < :cutoff")
	int deleteExpiredBefore(Instant cutoff);
}
