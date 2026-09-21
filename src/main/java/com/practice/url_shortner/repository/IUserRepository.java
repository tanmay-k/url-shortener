package com.practice.url_shortner.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.practice.url_shortner.entity.UserEntity;

@Repository
public interface IUserRepository extends JpaRepository<UserEntity, Integer> {

	Optional<UserEntity> findByUsername(String username);

	@Query("SELECT EXISTS(SELECT 1 FROM UserEntity u WHERE u.username = :userName)")
	boolean userExists(String userName);
}
