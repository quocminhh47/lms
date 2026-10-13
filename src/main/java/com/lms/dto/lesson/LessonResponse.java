package com.lms.dto.lesson;

import java.util.UUID;

import com.lms.entity.LessonType;

public record LessonResponse(
		UUID id,
		UUID sectionId,
		UUID courseId,
		String title,
		LessonType lessonType,
		String contentUrl,
		Integer durationSeconds,
		int sortOrder) {
}
