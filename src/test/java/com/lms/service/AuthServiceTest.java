package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lms.configuration.JwtProperties;
import com.lms.dto.auth.AuthResponse;
import com.lms.dto.auth.LoginRequest;
import com.lms.dto.auth.RegisterRequest;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.ConflictException;
import com.lms.exception.UnauthorizedException;
import com.lms.repository.UserRepository;
import com.lms.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	private PasswordEncoder passwordEncoder;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		JwtProperties properties = new JwtProperties();
		properties.setSecret("test-jwt-secret-key-with-32bytes-min");
		properties.setExpirationMs(3_600_000L);
		properties.validate();
		authService = new AuthService(userRepository, passwordEncoder, new JwtService(properties));
	}

	@Test
	void register_createsStudentWithHashedPassword() {
		when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuthResponse response = authService.register(new RegisterRequest(
				"New@Example.com",
				"password123",
				"New Student",
				null));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();

		assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
		assertThat(saved.getEmail()).isEqualTo("new@example.com");
		assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
		assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
	}

	@Test
	void register_rejectsDuplicateEmail() {
		when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest(
				"dup@example.com",
				"password123",
				"Dup",
				null)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void login_returnsTokenForValidCredentials() {
		User user = storedUser("login@example.com", "password123", UserRole.STUDENT, true);
		when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));

		AuthResponse response = authService.login(new LoginRequest("login@example.com", "password123"));

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.expiresInMs()).isEqualTo(3_600_000L);
	}

	@Test
	void login_rejectsInvalidPasswordWithGenericError() {
		User user = storedUser("login@example.com", "password123", UserRole.STUDENT, true);
		when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequest("login@example.com", "wrong-password")))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void login_rejectsUnknownEmailWithGenericError() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "password123")))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void login_rejectsInactiveUserWithGenericError() {
		User user = storedUser("inactive@example.com", "password123", UserRole.STUDENT, false);
		when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequest("inactive@example.com", "password123")))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("Invalid email or password");
	}

	private User storedUser(String email, String rawPassword, UserRole role, boolean active) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setFullName("Test User");
		user.setRole(role);
		user.setActive(active);
		return user;
	}
}
