package com.practice.url_shortner.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "click_event")
public class ClickEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "short_code", nullable = false)
	private String shortCode;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "clicked_at", nullable = false)
	private LocalDateTime clickedAt;
}
