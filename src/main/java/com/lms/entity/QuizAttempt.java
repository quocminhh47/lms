package com.lms.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttempt {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private QuizAttemptStatus status = QuizAttemptStatus.IN_PROGRESS;

	@Column(name = "score")
	private BigDecimal score;

	@Column(name = "is_passed")
	private Boolean passed;

	@Setter(AccessLevel.NONE)
	@Column(name = "started_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime startedAt;

	@Column(name = "submitted_at")
	private OffsetDateTime submittedAt;

	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;
}
