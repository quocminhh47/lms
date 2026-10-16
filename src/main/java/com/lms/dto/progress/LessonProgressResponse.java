package com.lms.dto.progress;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonProgressResponse(
		UUID lessonId,
		boolean isCompleted,
		int watchedDurationSeconds,
		OffsetDateTime lastAccessedAt) {
}
