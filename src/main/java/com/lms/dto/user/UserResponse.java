package com.lms.dto.user;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.lms.entity.UserRole;

public record UserResponse(
		UUID id,
		String email,
		String fullName,
		UserRole role,
		String avatarUrl,
		String phone,
		LocalDate dateOfBirth,
		boolean active,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
