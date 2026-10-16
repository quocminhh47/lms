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
import com.lms.util.PhoneNumbers;

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
		applyFullName(user, request.fullName().trim());
		user.setRole(request.role());
		user.setAvatarUrl(blankToNull(request.avatarUrl()));
		user.setPhone(resolvePhone(request.phone(), null));
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

		if (request.firstName() != null || request.lastName() != null) {
			String firstName = request.firstName() != null
					? requireNotBlank(request.firstName(), "firstName")
					: user.getFirstName();
			String lastName = request.lastName() != null
					? requireNotBlank(request.lastName(), "lastName")
					: user.getLastName();
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setFullName(joinName(firstName, lastName));
		} else if (request.fullName() != null) {
			applyFullName(user, requireNotBlank(request.fullName(), "fullName"));
		}
		if (request.avatarUrl() != null) {
			user.setAvatarUrl(blankToNull(request.avatarUrl()));
		}
		if (request.phone() != null) {
			user.setPhone(resolvePhone(request.phone(), user.getId()));
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

	/** Normalizes a phone number, validates its digit count and enforces uniqueness. */
	private String resolvePhone(String raw, UUID currentUserId) {
		String phone = PhoneNumbers.normalize(raw);
		if (phone == null) {
			return null;
		}
		if (!PhoneNumbers.isValid(phone)) {
			throw new IllegalArgumentException("phone must be a valid phone number (8-15 digits)");
		}
		boolean taken = userRepository.findByPhone(phone)
				.map(other -> !other.getId().equals(currentUserId))
				.orElse(false);
		if (taken) {
			throw new ConflictException("Phone number already registered", "phone");
		}
		return phone;
	}

	/**
	 * Sets the display name and a best-effort first/last split (first token / remainder).
	 * Name order differs between cultures, so callers that know the parts should set them directly.
	 */
	private static void applyFullName(User user, String fullName) {
		user.setFullName(fullName);
		int space = fullName.indexOf(' ');
		if (space < 0) {
			user.setFirstName(truncate(fullName));
			user.setLastName(null);
		} else {
			user.setFirstName(truncate(fullName.substring(0, space)));
			String rest = fullName.substring(space + 1).trim();
			user.setLastName(rest.isEmpty() ? null : truncate(rest));
		}
	}

	private static String joinName(String firstName, String lastName) {
		if (firstName == null || firstName.isBlank()) {
			return lastName;
		}
		if (lastName == null || lastName.isBlank()) {
			return firstName;
		}
		return firstName + " " + lastName;
	}

	private static String requireNotBlank(String value, String field) {
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return trimmed;
	}

	private static String truncate(String value) {
		return value.length() <= 100 ? value : value.substring(0, 100);
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
