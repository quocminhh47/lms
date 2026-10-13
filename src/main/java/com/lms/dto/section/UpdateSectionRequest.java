package com.lms.dto.section;

import jakarta.validation.constraints.Size;

public record UpdateSectionRequest(
		@Size(max = 255) String title,
		Integer sortOrder) {
}
