package com.lms.dto.lesson;

import com.lms.entity.LessonType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateLessonRequest(
		@Size(max = 255) String title,
		LessonType lessonType,
		String contentUrl,
		@Min(0) Integer durationSeconds,
		Integer sortOrder) {
}
