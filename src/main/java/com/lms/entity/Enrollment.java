package com.lms.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "enrollments",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_enrollments_user_course",
				columnNames = { "user_id", "course_id" }))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Enrollment {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private EnrollmentStatus status = EnrollmentStatus.PENDING;

	@Setter(AccessLevel.NONE)
	@Column(name = "enrolled_at", nullable = false, updatable = false)
	private OffsetDateTime enrolledAt;

	@PrePersist
	void onPersist() {
		if (enrolledAt == null) {
			enrolledAt = OffsetDateTime.now();
		}
	}
}
