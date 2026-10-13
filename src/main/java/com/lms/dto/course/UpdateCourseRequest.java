package com.lms.dto.course;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateCourseRequest(
		@Size(max = 255) String title,
		String description,
		@DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
		Boolean published) {
}
