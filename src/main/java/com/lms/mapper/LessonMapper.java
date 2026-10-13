package com.lms.mapper;

import org.springframework.stereotype.Component;

import com.lms.dto.lesson.LessonResponse;
import com.lms.entity.Lesson;

@Component
public class LessonMapper {

	public LessonResponse toResponse(Lesson lesson) {
		return new LessonResponse(
				lesson.getId(),
				lesson.getSection().getId(),
				lesson.getSection().getCourse().getId(),
				lesson.getTitle(),
				lesson.getLessonType(),
				lesson.getContentUrl(),
				lesson.getDurationSeconds(),
				lesson.getSortOrder());
	}
}
