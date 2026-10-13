package com.lms.dto.course;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CourseResponse(
		UUID id,
		String title,
		String description,
		UUID instructorId,
		BigDecimal price,
		boolean published,
		UUID finalQuizId,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
