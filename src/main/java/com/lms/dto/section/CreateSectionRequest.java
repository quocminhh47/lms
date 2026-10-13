package com.lms.dto.section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectionRequest(
		@NotBlank @Size(max = 255) String title,
		Integer sortOrder) {
}
