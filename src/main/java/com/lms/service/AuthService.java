package com.lms.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.auth.AuthResponse;
import com.lms.dto.auth.LoginRequest;
import com.lms.dto.auth.RegisterRequest;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.ConflictException;
import com.lms.exception.UnauthorizedException;
import com.lms.repository.UserRepository;
import com.lms.security.JwtService;
import com.lms.util.PhoneNumbers;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private static final String INVALID_CREDENTIALS = "Invalid credentials";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("Email already registered", "email");
		}
		String phone = PhoneNumbers.normalize(request.phoneNumber());
		if (phone == null) {
			throw new IllegalArgumentException("phoneNumber must not be blank");
		}
		if (userRepository.existsByPhone(phone)) {
			throw new ConflictException("Phone number already registered", "phoneNumber");
		}

		String firstName = request.firstName().trim();
		String lastName = request.lastName().trim();

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setFullName(firstName + " " + lastName);
		user.setPhone(phone);
		user.setDateOfBirth(request.dateOfBirth());
		user.setRole(UserRole.STUDENT);
		user.setAvatarUrl(blankToNull(request.avatarUrl()));
		user.setActive(true);

		User saved = userRepository.save(user);
		return issueToken(saved);
	}

	public AuthResponse login(LoginRequest request) {
		User user = resolveIdentifier(request.identifier().trim())
				.orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

		if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}

		return issueToken(user);
	}

	/** An identifier containing '@' is an email; anything else is treated as a phone number. */
	private Optional<User> resolveIdentifier(String identifier) {
		if (identifier.contains("@")) {
			return userRepository.findByEmail(normalizeEmail(identifier));
		}
		String phone = PhoneNumbers.normalize(identifier);
		return phone == null ? Optional.empty() : userRepository.findByPhone(phone);
	}

	private AuthResponse issueToken(User user) {
		String token = jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
		return new AuthResponse(token, "Bearer", jwtService.getExpirationMs());
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
