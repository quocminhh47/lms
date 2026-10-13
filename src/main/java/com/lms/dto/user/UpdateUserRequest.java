package com.lms.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@Size(max = 255) String fullName,
		@Size(max = 2048) String avatarUrl,
		Boolean active) {
}
