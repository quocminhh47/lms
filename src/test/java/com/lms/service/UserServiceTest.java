package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UpdateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.UserMapper;
import com.lms.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserMapper userMapper;

	@InjectMocks
	private UserService userService;

	@BeforeEach
	void setUp() {
		userMapper = new UserMapper();
		userService = new UserService(userRepository, userMapper, passwordEncoder);
	}

	@Test
	void create_hashesPasswordAndOmitsHashFromResponse() {
		CreateUserRequest request = new CreateUserRequest(
				"Alex@Example.com",
				"password123",
				"Alex Instructor",
				UserRole.INSTRUCTOR,
				null,
				"+84 912 345 678",
				LocalDate.of(1990, 5, 15));

		when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("hashed-secret");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userService.create(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();

		assertThat(saved.getEmail()).isEqualTo("alex@example.com");
		assertThat(saved.getPasswordHash()).isEqualTo("hashed-secret");
		assertThat(response.email()).isEqualTo("alex@example.com");
		assertThat(response.fullName()).isEqualTo("Alex Instructor");
		assertThat(response.role()).isEqualTo(UserRole.INSTRUCTOR);
		assertThat(saved.getPhone()).isEqualTo("+84912345678");
		assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
		assertThat(response.phone()).isEqualTo("+84912345678");
		assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
	}

	@Test
	void create_rejectsDuplicateEmail() {
		CreateUserRequest request = new CreateUserRequest(
				"dup@example.com",
				"password123",
				"Dup User",
				UserRole.STUDENT,
				null,
				null,
				null);
		when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.create(request))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("Email already registered");
	}

	@Test
	void getById_throwsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(userRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getById(id))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_changesProfileFields() {
		UUID id = UUID.randomUUID();
		User user = new User();
		user.setId(id);
		user.setEmail("user@example.com");
		user.setPasswordHash("hash");
		user.setFullName("Old Name");
		user.setRole(UserRole.STUDENT);
		user.setActive(true);

		when(userRepository.findById(id)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userService.update(
				id,
				new UpdateUserRequest(
						"New Name",
						null,
						null,
						"https://cdn/avatar.png",
						"0901234567",
						LocalDate.of(2000, 1, 1),
						false));

		assertThat(response.fullName()).isEqualTo("New Name");
		assertThat(response.avatarUrl()).isEqualTo("https://cdn/avatar.png");
		assertThat(response.active()).isFalse();
		assertThat(response.phone()).isEqualTo("0901234567");
		assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
	}

	@Test
	void update_clearsBlankPhone() {
		UUID id = UUID.randomUUID();
		User user = new User();
		user.setId(id);
		user.setEmail("user@example.com");
		user.setPasswordHash("hash");
		user.setFullName("User");
		user.setRole(UserRole.STUDENT);
		user.setPhone("+1");
		user.setActive(true);

		when(userRepository.findById(id)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userService.update(
				id,
				new UpdateUserRequest(null, null, null, null, "  ", null, null));

		assertThat(response.phone()).isNull();
	}
}
