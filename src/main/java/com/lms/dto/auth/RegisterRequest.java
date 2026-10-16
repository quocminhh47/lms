package com.lms.dto.auth;

import java.time.LocalDate;

import com.lms.util.PhoneNumbers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.PATTERN_MESSAGE)
		String phoneNumber,
		@NotNull @Past LocalDate dateOfBirth,
		@Size(max = 2048) String avatarUrl) {
}
