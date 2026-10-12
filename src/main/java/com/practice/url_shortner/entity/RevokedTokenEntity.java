package com.practice.url_shortner.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "revoked_token")
public class RevokedTokenEntity {

	@Id
	private String jti;

	@Column(name = "expires_at")
	private Instant expiresAt;
}
