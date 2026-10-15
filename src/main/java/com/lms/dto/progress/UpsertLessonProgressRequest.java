package com.lms.dto.progress;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertLessonProgressRequest(
		@NotNull @Min(0) Integer watchedDurationSeconds) {
}
