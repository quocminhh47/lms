package com.lms.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UpdateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.entity.User;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.UserMapper;
import com.lms.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(
			UserRepository userRepository,
			UserMapper userMapper,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("Email already registered: " + email);
		}

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName().trim());
		user.setRole(request.role());
		user.setAvatarUrl(blankToNull(request.avatarUrl()));
		user.setPhone(blankToNull(request.phone()));
		user.setDateOfBirth(request.dateOfBirth());
		user.setActive(true);

		return userMapper.toResponse(userRepository.save(user));
	}

	public UserResponse getById(UUID id) {
		return userMapper.toResponse(requireUser(id));
	}

	public UserResponse getByEmail(String email) {
		User user = userRepository.findByEmail(normalizeEmail(email))
				.orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));
		return userMapper.toResponse(user);
	}

	public List<UserResponse> list() {
		return userRepository.findAll().stream()
				.map(userMapper::toResponse)
				.toList();
	}

	@Transactional
	public UserResponse update(UUID id, UpdateUserRequest request) {
		User user = requireUser(id);

		if (request.fullName() != null) {
			String fullName = request.fullName().trim();
			if (fullName.isEmpty()) {
				throw new IllegalArgumentException("fullName must not be blank");
			}
			user.setFullName(fullName);
		}
		if (request.avatarUrl() != null) {
			user.setAvatarUrl(blankToNull(request.avatarUrl()));
		}
		if (request.phone() != null) {
			user.setPhone(blankToNull(request.phone()));
		}
		if (request.dateOfBirth() != null) {
			user.setDateOfBirth(request.dateOfBirth());
		}
		if (request.active() != null) {
			user.setActive(request.active());
		}

		return userMapper.toResponse(userRepository.save(user));
	}

	public User requireUser(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
