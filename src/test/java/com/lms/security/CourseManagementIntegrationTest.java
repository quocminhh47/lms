package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.UserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
class CourseManagementIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private EntityManager entityManager;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void createCourse_isAdminOnly() throws Exception {
		User instructor = seedUser("ins-" + UUID.randomUUID() + "@example.com", UserRole.INSTRUCTOR);
		User student = seedUser("stu-" + UUID.randomUUID() + "@example.com", UserRole.STUDENT);
		User admin = seedUser("adm-" + UUID.randomUUID() + "@example.com", UserRole.ADMIN);

		String body = courseCreateBody(instructor.getId(), "Admin Course", false);

		mockMvc.perform(post("/api/courses")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/courses")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/courses")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/courses")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Admin Course"))
				.andExpect(jsonPath("$.published").value(false))
				.andExpect(jsonPath("$.price").value(0));
	}

	@Test
	void publicCatalog_listsOnlyPublishedCourses() throws Exception {
		User instructor = seedUser("pub-ins-" + UUID.randomUUID() + "@example.com", UserRole.INSTRUCTOR);
		Course published = seedCourse(instructor, "Published Course", true);
		seedCourse(instructor, "Draft Course", false);

		mockMvc.perform(get("/api/courses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + published.getId() + "')]").exists())
				.andExpect(jsonPath("$[?(@.title=='Draft Course')]").doesNotExist());

		// Non-admin cannot force unpublished listing via query param
		mockMvc.perform(get("/api/courses").param("published", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.title=='Draft Course')]").doesNotExist());
	}

	@Test
	void getById_publicSeesPublishedOnly_adminSeesUnpublished() throws Exception {
		User instructor = seedUser("get-ins-" + UUID.randomUUID() + "@example.com", UserRole.INSTRUCTOR);
		User student = seedUser("get-stu-" + UUID.randomUUID() + "@example.com", UserRole.STUDENT);
		User admin = seedUser("get-adm-" + UUID.randomUUID() + "@example.com", UserRole.ADMIN);
		Course published = seedCourse(instructor, "Live", true);
		Course draft = seedCourse(instructor, "Hidden", false);

		mockMvc.perform(get("/api/courses/" + published.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Live"));

		mockMvc.perform(get("/api/courses/" + draft.getId()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/courses/" + draft.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/courses/" + draft.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/courses/" + draft.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Hidden"))
				.andExpect(jsonPath("$.published").value(false));

		mockMvc.perform(get("/api/courses").param("published", "false")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + draft.getId() + "')]").exists());
	}

	@Test
	void updatePublishUnpublish_areAdminOnly() throws Exception {
		User instructor = seedUser("mut-ins-" + UUID.randomUUID() + "@example.com", UserRole.INSTRUCTOR);
		User student = seedUser("mut-stu-" + UUID.randomUUID() + "@example.com", UserRole.STUDENT);
		User admin = seedUser("mut-adm-" + UUID.randomUUID() + "@example.com", UserRole.ADMIN);
		Course course = seedCourse(instructor, "Mutable", false);

		String updateBody = "{\"title\":\"Renamed\"}";

		mockMvc.perform(put("/api/courses/" + course.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateBody))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(put("/api/courses/" + course.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateBody))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/courses/" + course.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor))
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateBody))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/courses/" + course.getId() + "/publish")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/courses/" + course.getId() + "/unpublish")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor)))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/courses/" + course.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Renamed"));

		mockMvc.perform(post("/api/courses/" + course.getId() + "/publish")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(true));

		mockMvc.perform(post("/api/courses/" + course.getId() + "/unpublish")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(false));
	}

	@Test
	void create_rejectsInvalidPriceAndMissingCourseReturns404() throws Exception {
		User instructor = seedUser("val-ins-" + UUID.randomUUID() + "@example.com", UserRole.INSTRUCTOR);
		User admin = seedUser("val-adm-" + UUID.randomUUID() + "@example.com", UserRole.ADMIN);

		mockMvc.perform(post("/api/courses")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Bad","instructorId":"%s","price":-1,"published":false}
								""".formatted(instructor.getId())))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/courses")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Paid","instructorId":"%s","price":9.99,"published":false}
								""".formatted(instructor.getId())))
				.andExpect(status().isBadRequest());

		UUID missing = UUID.randomUUID();
		mockMvc.perform(get("/api/courses/" + missing))
				.andExpect(status().isNotFound());
		mockMvc.perform(put("/api/courses/" + missing)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"x\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void update_bumpsUpdatedAt() throws Exception {
		User instructor = seedUser("ts-ins-" + UUID.randomUUID() + "@example.com", UserRole.INSTRUCTOR);
		User admin = seedUser("ts-adm-" + UUID.randomUUID() + "@example.com", UserRole.ADMIN);
		Course course = seedCourse(instructor, "Timestamp", false);
		OffsetDateTime updatedBefore = course.getUpdatedAt();

		Thread.sleep(15L);

		mockMvc.perform(put("/api/courses/" + course.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Timestamp Updated\"}"))
				.andExpect(status().isOk());

		entityManager.clear();
		Course reloaded = courseRepository.findById(course.getId()).orElseThrow();
		assertThat(reloaded.getUpdatedAt()).isAfter(updatedBefore);
		assertThat(reloaded.getTitle()).isEqualTo("Timestamp Updated");
	}

	@Test
	void coursePublicGet_doesNotOpenOtherApis() throws Exception {
		mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/courses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/courses/" + UUID.randomUUID() + "/publish"))
				.andExpect(status().isUnauthorized());
	}

	private String courseCreateBody(UUID instructorId, String title, boolean published) {
		return """
				{"title":"%s","description":"desc","instructorId":"%s","price":0,"published":%s}
				""".formatted(title, instructorId, published);
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	private User seedUser(String email, UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName("Seed User");
		user.setRole(role);
		user.setActive(true);
		return userRepository.save(user);
	}

	private Course seedCourse(User instructor, String title, boolean published) {
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle(title);
		course.setDescription("desc");
		course.setInstructor(instructor);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(published);
		return courseRepository.save(course);
	}
}
