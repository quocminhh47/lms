package com.lms.mapper;

import org.springframework.stereotype.Component;

import com.lms.dto.user.UserResponse;
import com.lms.entity.User;

@Component
public class UserMapper {

	public UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getFirstName(),
				user.getLastName(),
				user.getRole(),
				user.getAvatarUrl(),
				user.getPhone(),
				user.getDateOfBirth(),
				user.isActive(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}
}
