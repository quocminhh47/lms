package com.lms.dto.user;

import java.time.LocalDate;

import com.lms.entity.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotBlank @Size(max = 255) String fullName,
		@NotNull UserRole role,
		@Size(max = 2048) String avatarUrl,
		@Size(max = 32) @Pattern(regexp = "^[+0-9\\s().-]*$", message = "phone has invalid characters")
		String phone,
		@Past LocalDate dateOfBirth) {
}
