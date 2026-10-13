package com.lms.dto.lesson;

import com.lms.entity.LessonType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLessonRequest(
		@NotBlank @Size(max = 255) String title,
		@NotNull LessonType lessonType,
		String contentUrl,
		@Min(0) Integer durationSeconds,
		Integer sortOrder) {
}
