package com.practice.url_shortner.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * One link in a refresh-token chain. All links of a login (a "session") share a
 * {@code familyId} and the same absolute {@code expiresAt}; each rotation marks
 * the presented link {@code used} and adds a new one.
 */
@Data
@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "family_id")
	private String familyId;

	@Column(name = "user_id")
	private int userId;

	/** SHA-256 hex of the opaque token; the token itself is never stored. */
	@Column(name = "token_hash")
	private String tokenHash;

	@Column(name = "expires_at")
	private Instant expiresAt;

	private boolean used;

	private boolean revoked;

	/** {@code jti} of the access token issued together with this refresh token. */
	@Column(name = "current_access_jti")
	private String currentAccessJti;

	/** Upper bound on when that access token expires; used when denylisting it. */
	@Column(name = "access_expires_at")
	private Instant accessExpiresAt;

	@Column(name = "created_at")
	private Instant createdAt;
}
