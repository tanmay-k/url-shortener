package com.practice.url_shortner.entity;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private @Nullable String password;

	@Column(unique = true)
	private String username;

	//	private Set<GrantedAuthority> authorities

	@Column(name = "account_non_expired")
	private boolean accountNonExpired;

	@Column(name = "account_non_locked")
	private boolean accountNonLocked;

	@Column(name = "credentials_non_expired")
	private boolean credentialsNonExpired;

	private boolean enabled;

	/** SHA-256 of the one-time recovery code; null until the user has one. */
	@Column(name = "recovery_code_hash")
	private @Nullable String recoveryCodeHash;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
