package com.practice.url_shortner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.practice.url_shortner.entity.UrlMappingEntity;

@Repository
public interface IUrlMappingRepository extends JpaRepository<UrlMappingEntity, Integer> {

	Optional<UrlMappingEntity> findByShortCode(String shortCode);

	Optional<UrlMappingEntity> findByShortCodeAndEnabled(String shortCode, boolean enabled);
}
