package com.lms.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "lesson_progress",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_lesson_progress_user_lesson",
				columnNames = { "user_id", "lesson_id" }))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonProgress {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@Column(name = "is_completed", nullable = false)
	private boolean completed = false;

	@Column(name = "watched_duration_seconds", nullable = false)
	private int watchedDurationSeconds = 0;

	@Column(name = "last_accessed_at")
	private OffsetDateTime lastAccessedAt;
}
