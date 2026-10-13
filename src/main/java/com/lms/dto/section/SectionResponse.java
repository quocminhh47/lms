package com.lms.dto.section;

import java.util.UUID;

public record SectionResponse(
		UUID id,
		UUID courseId,
		String title,
		int sortOrder) {
}
