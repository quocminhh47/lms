package com.lms.dto.course;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
		@NotBlank @Size(max = 255) String title,
		String description,
		@NotNull UUID instructorId,
		@NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
		@Size(max = 2048) String thumbnailUrl,
		Boolean published) {
}
