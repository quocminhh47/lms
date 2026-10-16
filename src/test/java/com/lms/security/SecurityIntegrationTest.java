package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.ErrorResponse;
import com.lms.exception.GlobalExceptionHandler;
import com.lms.repository.UserRepository;

import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import jakarta.servlet.http.HttpServletRequest;

@SpringBootTest
class SecurityIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private EntityManager entityManager;

	private MockMvc mockMvc;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void register_isPublicAndAlwaysCreatesStudent() throws Exception {
		String email = "reg-" + UUID.randomUUID() + "@example.com";
		String body = """
				{
				  "email": "%s",
				  "password": "password123",
				  "firstName": "Reg",
				  "lastName": "Student",
				  "phoneNumber": "+84 %s",
				  "dateOfBirth": "2000-01-15",
				  "role": "ADMIN"
				}
				""".formatted(email, uniquePhoneDigits());

		MvcResult result = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andReturn();

		User saved = userRepository.findByEmail(email).orElseThrow();
		assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
		assertThat(saved.getFirstName()).isEqualTo("Reg");
		assertThat(saved.getLastName()).isEqualTo("Student");
		assertThat(saved.getFullName()).isEqualTo("Reg Student");
		assertThat(saved.getPhone()).startsWith("+84");
		assertThat(saved.getDateOfBirth()).isEqualTo(java.time.LocalDate.of(2000, 1, 15));
		assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();

		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(json.has("password")).isFalse();
		assertThat(json.has("passwordHash")).isFalse();
	}

	@Test
	void login_issuesJwt_andProtectedEndpointRequiresAuth() throws Exception {
		String email = "login-" + UUID.randomUUID() + "@example.com";
		seedUser(email, "password123", UserRole.STUDENT);

		mockMvc.perform(get("/api/users"))
				.andExpect(status().isUnauthorized());

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password123"}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();

		String token = objectMapper.readTree(login.getResponse().getContentAsString())
				.get("accessToken")
				.asText();

		User self = userRepository.findByEmail(email).orElseThrow();
		mockMvc.perform(get("/api/users/" + self.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void createUser_isAdminOnly() throws Exception {
		User student = seedUser("stu-" + UUID.randomUUID() + "@example.com", "password123", UserRole.STUDENT);
		User instructor = seedUser("ins-" + UUID.randomUUID() + "@example.com", "password123", UserRole.INSTRUCTOR);
		User admin = seedUser("adm-" + UUID.randomUUID() + "@example.com", "password123", UserRole.ADMIN);

		String newEmail = "created-" + UUID.randomUUID() + "@example.com";
		String body = """
				{"email":"%s","password":"password123","fullName":"Created","role":"INSTRUCTOR"}
				""".formatted(newEmail);

		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());
		assertThat(userRepository.existsByEmail(newEmail)).isFalse();

		mockMvc.perform(post("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("INSTRUCTOR"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void profileUpdate_cannotChangeOwnRoleOrActiveFlag() throws Exception {
		User student = seedUser("self-" + UUID.randomUUID() + "@example.com", "password123", UserRole.STUDENT);
		String token = tokenFor(student);

		mockMvc.perform(put("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Renamed","role":"ADMIN"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Renamed"))
				.andExpect(jsonPath("$.role").value("STUDENT"));
		assertThat(userRepository.findById(student.getId()).orElseThrow().getRole())
				.isEqualTo(UserRole.STUDENT);

		mockMvc.perform(put("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"active":false}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void userManagement_enforcesOwnership() throws Exception {
		User studentA = seedUser("a-" + UUID.randomUUID() + "@example.com", "password123", UserRole.STUDENT);
		User studentB = seedUser("b-" + UUID.randomUUID() + "@example.com", "password123", UserRole.STUDENT);
		User admin = seedUser("adm-" + UUID.randomUUID() + "@example.com", "password123", UserRole.ADMIN);
		String tokenA = tokenFor(studentA);

		mockMvc.perform(get("/api/users/" + studentB.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/users/" + studentB.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fullName\":\"Hacked\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/users/" + studentB.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void protectedUserEndpoints_rejectUnauthenticated() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/users/" + id)).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/users/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fullName\":\"x\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authMe_requiresAuthenticationAndReturnsCurrentUser() throws Exception {
		User student = seedUser(
				"me-" + UUID.randomUUID() + "@example.com",
				"password123",
				UserRole.STUDENT);

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(student.getId().toString()))
				.andExpect(jsonPath("$.email").value(student.getEmail()))
				.andExpect(jsonPath("$.fullName").value("Seed User"))
				.andExpect(jsonPath("$.role").value("STUDENT"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void authMe_rejectsInactiveUserJwt() throws Exception {
		User student = seedUser(
				"me-inact-" + UUID.randomUUID() + "@example.com",
				"password123",
				UserRole.STUDENT);
		User admin = seedUser(
				"me-inact-adm-" + UUID.randomUUID() + "@example.com",
				"password123",
				UserRole.ADMIN);
		String studentToken = tokenFor(student);

		mockMvc.perform(put("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deactivatedUser_existingJwtIsRejectedOnProtectedEndpoint() throws Exception {
		User student = seedUser(
				"deact-" + UUID.randomUUID() + "@example.com",
				"password123",
				UserRole.STUDENT);
		User admin = seedUser(
				"deact-adm-" + UUID.randomUUID() + "@example.com",
				"password123",
				UserRole.ADMIN);
		String studentToken = tokenFor(student);

		mockMvc.perform(get("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
		assertThat(userRepository.findById(student.getId()).orElseThrow().isActive()).isFalse();

		// JwtAuthenticationFilter skips inactive users, so the request is unauthenticated → 401
		mockMvc.perform(get("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void register_rejectsDuplicateEmailWith409() throws Exception {
		String email = "dup-reg-" + UUID.randomUUID() + "@example.com";
		seedUser(email, "password123", UserRole.STUDENT);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password123","firstName":"Dup","lastName":"User",
								 "phoneNumber":"%s","dateOfBirth":"2000-01-15"}
								""".formatted(email, uniquePhoneDigits())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Email already registered"))
				.andExpect(jsonPath("$.details[0]").value("email: Email already registered"));
	}

	@Test
	void createUser_rejectsDuplicateEmailWith409() throws Exception {
		String email = "dup-create-" + UUID.randomUUID() + "@example.com";
		seedUser(email, "password123", UserRole.STUDENT);
		User admin = seedUser(
				"dup-adm-" + UUID.randomUUID() + "@example.com",
				"password123",
				UserRole.ADMIN);

		mockMvc.perform(post("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password123","fullName":"Dup","role":"STUDENT"}
								""".formatted(email)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Email already registered: " + email));
	}

	@Test
	void duplicateEmail_persistenceConflict_mapsTo409() {
		String email = "db-dup-" + UUID.randomUUID() + "@example.com";
		seedUser(email, "password123", UserRole.STUDENT);

		User clash = new User();
		clash.setId(UUID.randomUUID());
		clash.setEmail(email);
		clash.setPasswordHash(passwordEncoder.encode("password123"));
		clash.setFullName("Clash");
		clash.setRole(UserRole.STUDENT);
		clash.setActive(true);

		DataIntegrityViolationException thrown = null;
		try {
			userRepository.saveAndFlush(clash);
		}
		catch (DataIntegrityViolationException ex) {
			thrown = ex;
		}
		assertThat(thrown).isNotNull();
		assertThat(GlobalExceptionHandler.isUsersEmailUniqueViolation(thrown)).isTrue();

		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		org.mockito.Mockito.when(request.getRequestURI()).thenReturn("/api/users");
		ResponseEntity<ErrorResponse> response =
				new GlobalExceptionHandler().handleDataIntegrity(thrown, request);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Email already registered");

		entityManager.clear();
	}

	@Test
	void profileUpdate_bumpsUpdatedAt() throws Exception {
		User student = seedUser(
				"ts-" + UUID.randomUUID() + "@example.com",
				"password123",
				UserRole.STUDENT);
		OffsetDateTime updatedBefore = student.getUpdatedAt();

		Thread.sleep(15L);

		mockMvc.perform(put("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fullName\":\"Timestamped\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Timestamped"));

		entityManager.clear();
		User reloaded = userRepository.findById(student.getId()).orElseThrow();
		assertThat(reloaded.getUpdatedAt()).isAfter(updatedBefore);
	}

	@Test
	void login_rejectsInvalidCredentials() throws Exception {
		String email = "bad-" + UUID.randomUUID() + "@example.com";
		seedUser(email, "password123", UserRole.STUDENT);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"wrong-password"}
								""".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid credentials"));
	}

	@Test
	void expiredOrInvalidJwt_isRejected() throws Exception {
		mockMvc.perform(get("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());

		User student = seedUser("exp-" + UUID.randomUUID() + "@example.com", "password123", UserRole.STUDENT);
		String expired = createExpiredToken(student);

		mockMvc.perform(get("/api/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void courseCreate_requiresAdminRole() throws Exception {
		User student = seedUser("stu-" + UUID.randomUUID() + "@example.com", "password123", UserRole.STUDENT);
		User admin = seedUser("adm-" + UUID.randomUUID() + "@example.com", "password123", UserRole.ADMIN);
		User instructor = seedUser("ins-" + UUID.randomUUID() + "@example.com", "password123", UserRole.INSTRUCTOR);

		String studentToken = jwtService.createToken(student.getId(), student.getEmail(), student.getRole());
		String adminToken = jwtService.createToken(admin.getId(), admin.getEmail(), admin.getRole());

		String courseBody = """
				{
				  "title": "Admin Course",
				  "description": "Created by admin",
				  "instructorId": "%s",
				  "price": 0,
				  "published": false
				}
				""".formatted(instructor.getId());

		mockMvc.perform(post("/api/courses")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(courseBody))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/courses")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(courseBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Admin Course"));
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	/** 10-digit unique-ish suffix for phone numbers (unique per call within a run). */
	private static String uniquePhoneDigits() {
		return String.valueOf(900_000_000L + (System.nanoTime() % 99_000_000L));
	}

	private User seedUser(String email, String rawPassword, UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setFullName("Seed User");
		user.setRole(role);
		user.setActive(true);
		return userRepository.save(user);
	}

	private String createExpiredToken(User user) {
		com.lms.configuration.JwtProperties props = new com.lms.configuration.JwtProperties();
		props.setSecret("test-jwt-secret-key-with-32bytes-min");
		props.setExpirationMs(1L);
		JwtService shortLived = new JwtService(props);
		String token = shortLived.createToken(user.getId(), user.getEmail(), user.getRole());
		try {
			Thread.sleep(5L);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return token;
	}
}
