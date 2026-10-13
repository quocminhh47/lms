package com.lms.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lms.dto.course.CourseResponse;
import com.lms.entity.Course;

@Component
public class CourseMapper {

	public CourseResponse toResponse(Course course) {
		UUID finalQuizId = course.getFinalQuiz() != null ? course.getFinalQuiz().getId() : null;
		return new CourseResponse(
				course.getId(),
				course.getTitle(),
				course.getDescription(),
				course.getInstructor().getId(),
				course.getPrice(),
				course.isPublished(),
				finalQuizId,
				course.getCreatedAt(),
				course.getUpdatedAt());
	}
}
