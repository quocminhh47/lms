package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.repository.UserRepository;

@SpringBootTest
class ArticleIntegrationTest {

	private static final String BODY = "<p>Visible body</p>";
	private static final String UPDATED_BODY = "<p>Pending body</p>";

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void publicListAndDetail_areAnonymous() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		String token = tokenFor(student);

		JsonNode created = createArticle(token, "Public Article", BODY);
		String id = created.get("id").asText();
		String slug = created.get("slug").asText();

		mockMvc.perform(post("/api/articles/" + id + "/submit").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		User admin = seedUser(UserRole.ADMIN);
		mockMvc.perform(post("/api/admin/articles/" + id + "/approve")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/public/articles")).andExpect(status().isOk());
		mockMvc.perform(get("/api/public/articles/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Public Article"))
				.andExpect(jsonPath("$.contentHtml", containsString("Visible body")))
				.andExpect(jsonPath("$.rejectionReason").doesNotExist());
	}

	@Test
	void manageEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/articles/my")).andExpect(status().isUnauthorized());
	}

	@Test
	void studentManagesOwnArticle_notAnotherAuthors() throws Exception {
		User author = seedUser(UserRole.STUDENT);
		User other = seedUser(UserRole.STUDENT);
		JsonNode created = createArticle(tokenFor(author), "Mine", BODY);
		String id = created.get("id").asText();

		mockMvc.perform(get("/api/articles/" + id + "/manage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(author)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Mine"));

		mockMvc.perform(get("/api/articles/" + id + "/manage")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(other)))
				.andExpect(status().isNotFound());
	}

	@Test
	void studentCannotCallAdminEndpoints() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		JsonNode created = createArticle(tokenFor(student), "Draft", BODY);
		String id = created.get("id").asText();

		mockMvc.perform(post("/api/admin/articles/" + id + "/approve")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanApproveRejectPublishAndArchive() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		User admin = seedUser(UserRole.ADMIN);
		String studentToken = tokenFor(student);
		JsonNode created = createArticle(studentToken, "Workflow", BODY);
		String id = created.get("id").asText();
		String slug = created.get("slug").asText();

		mockMvc.perform(post("/api/articles/" + id + "/submit").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/articles/" + id + "/approve")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"));

		mockMvc.perform(put("/api/articles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Workflow",
								  "contentHtml": "%s"
								}
								""".formatted(UPDATED_BODY)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

		mockMvc.perform(get("/api/public/articles/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentHtml", containsString("Visible body")));

		mockMvc.perform(post("/api/admin/articles/" + id + "/reject")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason":"Needs edits"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/public/articles/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentHtml", containsString("Visible body")));

		mockMvc.perform(post("/api/admin/articles/" + id + "/archive")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/public/articles/" + slug)).andExpect(status().isNotFound());
	}

	@Test
	void rejectValidationRequiresReason() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		User admin = seedUser(UserRole.ADMIN);
		JsonNode created = createArticle(tokenFor(student), "Validate", BODY);
		String id = created.get("id").asText();

		mockMvc.perform(post("/api/articles/" + id + "/submit")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/articles/" + id + "/reject")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason":"   "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	void adminCanPublishDraftDirectly() throws Exception {
		User admin = seedUser(UserRole.ADMIN);
		String token = tokenFor(admin);
		JsonNode created = createArticle(token, "Direct", BODY, true);
		String slug = created.get("slug").asText();

		assertThat(created.get("status").asText()).isEqualTo("PUBLISHED");

		mockMvc.perform(get("/api/public/articles/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Direct"));
	}

    @Test
    void instructorCannotBypassModerationOrReadPrivateDraft() throws Exception {
        User author = seedUser(UserRole.STUDENT);
        String id = createArticle(tokenFor(author), "Private", BODY).get("id").asText();
        String instructorToken = tokenFor(seedUser(UserRole.INSTRUCTOR));
        mockMvc.perform(get("/api/articles/" + id + "/manage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + instructorToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.contentHtml").doesNotExist());
        for (String path : new String[] { "/api/admin/articles", "/api/admin/articles/pending" }) {
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + instructorToken))
                    .andExpect(status().isForbidden());
        }
        for (String action : new String[] { "approve", "publish", "archive", "reject" }) {
            mockMvc.perform(post("/api/admin/articles/" + id + "/" + action)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + instructorToken)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Denied\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bypass\",\"contentHtml\":\"<p>Body</p>\",\"publishImmediately\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectedDraftCanBeEditedResubmittedAndApproved() throws Exception {
        String author = tokenFor(seedUser(UserRole.STUDENT));
        String admin = tokenFor(seedUser(UserRole.ADMIN));
        JsonNode created = createArticle(author, "Resubmit", BODY);
        String id = created.get("id").asText();
        String slug = created.get("slug").asText();
        mockMvc.perform(get("/api/public/articles/" + slug)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/articles/" + id + "/submit").header(HttpHeaders.AUTHORIZATION, "Bearer " + author))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/articles/" + id + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Add sources\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/articles/" + id + "/manage").header(HttpHeaders.AUTHORIZATION, "Bearer " + author))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rejectionReason").value("Add sources"));
        mockMvc.perform(put("/api/articles/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + author)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Resubmit\",\"contentHtml\":\"<p>Revised</p>\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/articles/" + id + "/submit").header(HttpHeaders.AUTHORIZATION, "Bearer " + author))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mockMvc.perform(post("/api/admin/articles/" + id + "/approve").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/articles/" + slug)).andExpect(status().isOk())
                .andExpect(jsonPath("$.contentHtml", containsString("Revised")));
    }

	private JsonNode createArticle(String token, String title, String contentHtml) throws Exception {
		return createArticle(token, title, contentHtml, false);
	}

	private JsonNode createArticle(String token, String title, String contentHtml, boolean publishImmediately)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/api/articles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "contentHtml": "%s",
								  "publishImmediately": %s
								}
								""".formatted(title, contentHtml, publishImmediately)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private User seedUser(UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName(role.name() + " User");
		user.setRole(role);
		user.setActive(true);
		return userRepository.save(user);
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}
}
