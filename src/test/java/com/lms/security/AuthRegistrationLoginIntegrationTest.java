package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.lms.entity.User;
import com.lms.repository.UserRepository;

@SpringBootTest
class AuthRegistrationLoginIntegrationTest {

	private static final AtomicLong PHONE_SEQ = new AtomicLong(100_000_000L);

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void register_persistsProfileFieldsAndNormalizesPhone() throws Exception {
		String email = uniqueEmail();
		String digits = nextPhoneDigits();

		register(email, "  Minh ", " Pham ", "+84 " + digits, "1999-05-20", "password123")
				.andExpect(status().isCreated());

		User saved = userRepository.findByEmail(email).orElseThrow();
		assertThat(saved.getFirstName()).isEqualTo("Minh");
		assertThat(saved.getLastName()).isEqualTo("Pham");
		assertThat(saved.getFullName()).isEqualTo("Minh Pham");
		assertThat(saved.getPhone()).isEqualTo("+84" + digits);
		assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1999, 5, 20));
	}

	@Test
	void register_returnsFieldDetailsForEveryInvalidField() throws Exception {
		String email = "not-an-email";
		long before = userRepository.count();

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"short","firstName":"  ","lastName":"",
								 "phoneNumber":"abc","dateOfBirth":"%s"}
								""".formatted(email, LocalDate.now().plusDays(1))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.details[?(@ =~ /^email: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^password: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^firstName: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^lastName: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^phoneNumber: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^dateOfBirth: .*/)]").isNotEmpty());

		assertThat(userRepository.count()).isEqualTo(before);
	}

	@Test
	void register_rejectsMissingProfileFields() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password123"}
								""".formatted(uniqueEmail())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details[?(@ =~ /^firstName: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^lastName: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^phoneNumber: .*/)]").isNotEmpty())
				.andExpect(jsonPath("$.details[?(@ =~ /^dateOfBirth: .*/)]").isNotEmpty());
	}

	@Test
	void register_rejectsDuplicatePhoneEvenWhenFormattedDifferently() throws Exception {
		String digits = nextPhoneDigits();
		register(uniqueEmail(), "A", "One", "+84" + digits, "2000-01-01", "password123")
				.andExpect(status().isCreated());

		register(uniqueEmail(), "B", "Two", "+84 " + digits.substring(0, 3) + "-" + digits.substring(3),
				"2000-01-01", "password123")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.details[0]").value("phoneNumber: Phone number already registered"));
	}

	@Test
	void login_worksWithEmailPhoneAndLegacyEmailProperty() throws Exception {
		String email = uniqueEmail();
		String digits = nextPhoneDigits();
		register(email, "Log", "In", "+84" + digits, "2000-01-01", "password123")
				.andExpect(status().isCreated());

		login("identifier", email.toUpperCase()).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
		login("identifier", "+84 " + digits).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
		login("email", email).andExpect(status().isOk());
	}

	@Test
	void login_doesNotRevealWhichIdentifierExists() throws Exception {
		String email = uniqueEmail();
		String digits = nextPhoneDigits();
		register(email, "Log", "In", "+84" + digits, "2000-01-01", "password123")
				.andExpect(status().isCreated());

		String wrongPassword = """
				{"identifier":"%s","password":"wrong-password"}
				""".formatted(email);
		String unknownPhone = """
				{"identifier":"+84999999999","password":"wrong-password"}
				""";
		String unknownEmail = """
				{"identifier":"nobody-%s@example.com","password":"wrong-password"}
				""".formatted(UUID.randomUUID());

		for (String body : new String[] { wrongPassword, unknownPhone, unknownEmail }) {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.message").value("Invalid credentials"));
		}
	}

	@Test
	void login_rejectsBlankIdentifier() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"identifier":"  ","password":"password123"}
								"""))
				.andExpect(status().isBadRequest());
	}

	private ResultActions register(
			String email, String first, String last, String phone, String dob, String password)
			throws Exception {
		return mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s","firstName":"%s","lastName":"%s",
						 "phoneNumber":"%s","dateOfBirth":"%s"}
						""".formatted(email, password, first, last, phone, dob)));
	}

	private ResultActions login(String property, String identifier) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"%s":"%s","password":"password123"}
						""".formatted(property, identifier)));
	}

	private static String uniqueEmail() {
		return "reg-" + UUID.randomUUID() + "@example.com";
	}

	private static String nextPhoneDigits() {
		return String.valueOf(PHONE_SEQ.incrementAndGet() + 800_000_000L);
	}
}
