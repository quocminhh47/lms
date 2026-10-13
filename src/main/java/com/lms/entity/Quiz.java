package com.lms.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false, unique = true)
	private Lesson lesson;

	/**
	 * Passing threshold 0–100. DB default exists for insert safety only.
	 * Service must set the appropriate value on create:
	 * section completion quizzes typically 100; Final Assessment typically 80
	 * (configurable); other quizzes as needed.
	 */
	@Column(name = "passing_score", nullable = false)
	private int passingScore = 70;

	@Column(name = "time_limit_minutes")
	private Integer timeLimitMinutes;

	@Column(name = "max_attempts")
	private Integer maxAttempts;
}
