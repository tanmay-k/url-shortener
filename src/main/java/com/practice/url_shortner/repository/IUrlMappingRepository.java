package com.practice.url_shortner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.practice.url_shortner.entity.UrlMappingEntity;

@Repository
public interface IUrlMappingRepository extends JpaRepository<UrlMappingEntity, Integer> {

	Optional<UrlMappingEntity> findByShortCode(String shortCode);

	/** The mapping for the short code, if both the mapping and the user who created it are enabled. */
	@Query("""
			SELECT m FROM UrlMappingEntity m
			WHERE m.shortCode = :shortCode AND m.enabled = true
			AND EXISTS (SELECT 1 FROM UserEntity u WHERE u.id = m.createdBy AND u.enabled = true)""")
	Optional<UrlMappingEntity> findActiveByShortCode(String shortCode);
}
