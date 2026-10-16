package com.lms.dto.enrollment;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.lms.entity.EnrollmentStatus;

public record EnrollmentResponse(
		UUID id,
		UUID userId,
		UUID courseId,
		String courseTitle,
		EnrollmentStatus status,
		OffsetDateTime enrolledAt) {
}
