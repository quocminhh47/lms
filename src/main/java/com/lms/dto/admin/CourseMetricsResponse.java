package com.lms.dto.admin;

public record CourseMetricsResponse(
		long total,
		long published,
		long draft,
		long withLessons,
		long emptyCurriculum) {
}
