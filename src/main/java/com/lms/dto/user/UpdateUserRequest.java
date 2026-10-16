package com.lms.dto.user;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@Size(max = 255) String fullName,
		@Size(max = 100) String firstName,
		@Size(max = 100) String lastName,
		@Size(max = 2048) String avatarUrl,
		@Size(max = 32) @Pattern(regexp = "^[+0-9\\s().-]*$", message = "phone has invalid characters")
		String phone,
		@Past LocalDate dateOfBirth,
		Boolean active) {
}
