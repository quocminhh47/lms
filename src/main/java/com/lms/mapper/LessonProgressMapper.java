package com.lms.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lms.dto.progress.LessonProgressResponse;
import com.lms.entity.LessonProgress;

@Component
public class LessonProgressMapper {

	public LessonProgressResponse toResponse(LessonProgress progress) {
		return new LessonProgressResponse(
				progress.getLesson().getId(),
				progress.isCompleted(),
				progress.getWatchedDurationSeconds(),
				progress.getLastAccessedAt());
	}

	public LessonProgressResponse unstarted(UUID lessonId) {
		return new LessonProgressResponse(lessonId, false, 0, null);
	}
}
