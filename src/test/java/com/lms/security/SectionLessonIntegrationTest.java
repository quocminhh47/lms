package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.lms.entity.Course;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Section;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.LessonRepository;
import com.lms.repository.SectionRepository;
import com.lms.repository.UserRepository;

@SpringBootTest
class SectionLessonIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private LessonRepository lessonRepository;

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
	void createSectionAndLesson_isAdminOnly() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User student = seedUser(UserRole.STUDENT);
		User admin = seedUser(UserRole.ADMIN);
		Course course = seedCourse(instructor, false);

		String sectionBody = "{\"title\":\"Week 1\",\"sortOrder\":1}";
		String sectionUrl = "/api/courses/" + course.getId() + "/sections";

		mockMvc.perform(post(sectionUrl).contentType(MediaType.APPLICATION_JSON).content(sectionBody))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post(sectionUrl)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(sectionBody))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(sectionUrl)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor))
						.contentType(MediaType.APPLICATION_JSON)
						.content(sectionBody))
				.andExpect(status().isForbidden());

		MvcResult createdSection = mockMvc.perform(post(sectionUrl)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(sectionBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Week 1"))
				.andExpect(jsonPath("$.sortOrder").value(1))
				.andReturn();

		UUID sectionId = UUID.fromString(
				objectMapper.readTree(createdSection.getResponse().getContentAsString()).get("id").asText());

		String lessonBody = """
				{"title":"Intro Video","lessonType":"VIDEO","contentUrl":"https://cdn/v","durationSeconds":60,"sortOrder":0}
				""";
		String lessonUrl = sectionUrl + "/" + sectionId + "/lessons";

		mockMvc.perform(post(lessonUrl)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(lessonBody))
				.andExpect(status().isForbidden());

		mockMvc.perform(post(lessonUrl)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(lessonBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lessonType").value("VIDEO"))
				.andExpect(jsonPath("$.courseId").value(course.getId().toString()));
	}

	@Test
	void publicCanReadPublishedContent_notUnpublished() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User admin = seedUser(UserRole.ADMIN);
		Course published = seedCourse(instructor, true);
		Course draft = seedCourse(instructor, false);
		Section pubSection = seedSection(published, "Pub Section", 0);
		Section draftSection = seedSection(draft, "Draft Section", 0);
		Lesson pubLesson = seedLesson(pubSection, "Pub Lesson", 0);
		Lesson draftLesson = seedLesson(draftSection, "Draft Lesson", 0);

		mockMvc.perform(get("/api/courses/" + published.getId() + "/sections"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Pub Section"));

		mockMvc.perform(get("/api/courses/" + published.getId() + "/sections/" + pubSection.getId()
						+ "/lessons/" + pubLesson.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Pub Lesson"));

		mockMvc.perform(get("/api/lessons/" + pubLesson.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(pubLesson.getId().toString()));

		mockMvc.perform(get("/api/courses/" + draft.getId() + "/sections"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/courses/" + draft.getId() + "/sections/" + draftSection.getId()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/lessons/" + draftLesson.getId()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/courses/" + draft.getId() + "/sections")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Draft Section"));
		mockMvc.perform(get("/api/lessons/" + draftLesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Draft Lesson"));
	}

	@Test
	void studentAndInstructor_cannotMutateOrReadUnpublished() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User student = seedUser(UserRole.STUDENT);
		Course draft = seedCourse(instructor, false);
		Section section = seedSection(draft, "S", 0);
		Lesson lesson = seedLesson(section, "L", 0);

		mockMvc.perform(get("/api/courses/" + draft.getId() + "/sections/" + section.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/lessons/" + lesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor)))
				.andExpect(status().isNotFound());

		mockMvc.perform(put("/api/courses/" + draft.getId() + "/sections/" + section.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(instructor))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Hacked\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/courses/" + draft.getId() + "/sections/" + section.getId()
						+ "/lessons/" + lesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Hacked\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void invalidHierarchy_returns404() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User admin = seedUser(UserRole.ADMIN);
		Course courseA = seedCourse(instructor, true);
		Course courseB = seedCourse(instructor, true);
		Section sectionA = seedSection(courseA, "A", 0);
		Lesson lessonA = seedLesson(sectionA, "LA", 0);
		Section sectionB = seedSection(courseB, "B", 0);

		mockMvc.perform(get("/api/courses/" + courseB.getId() + "/sections/" + sectionA.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/courses/" + courseA.getId() + "/sections/" + sectionB.getId()
						+ "/lessons/" + lessonA.getId()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/courses/" + courseA.getId() + "/sections/" + sectionA.getId()
						+ "/lessons/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	@Test
	void orderingUpdates_work() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User admin = seedUser(UserRole.ADMIN);
		Course course = seedCourse(instructor, true);
		Section section = seedSection(course, "S", 0);
		Lesson lesson = seedLesson(section, "L", 0);

		mockMvc.perform(put("/api/courses/" + course.getId() + "/sections/" + section.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sortOrder\":7}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sortOrder").value(7));

		mockMvc.perform(put("/api/courses/" + course.getId() + "/sections/" + section.getId()
						+ "/lessons/" + lesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sortOrder\":4}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sortOrder").value(4));

		assertThat(sectionRepository.findById(section.getId()).orElseThrow().getSortOrder()).isEqualTo(7);
		assertThat(lessonRepository.findById(lesson.getId()).orElseThrow().getSortOrder()).isEqualTo(4);
	}

	@Test
	void missingResources_and_unrelatedApisRemainProtected() throws Exception {
		mockMvc.perform(get("/api/courses/" + UUID.randomUUID() + "/sections"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/lessons/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/courses/" + UUID.randomUUID() + "/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"x\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminCanReorderAcrossList() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User admin = seedUser(UserRole.ADMIN);
		Course course = seedCourse(instructor, true);
		Section first = seedSection(course, "First", 0);
		Section second = seedSection(course, "Second", 1);

		mockMvc.perform(put("/api/courses/" + course.getId() + "/sections/" + first.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sortOrder\":5}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/courses/" + course.getId() + "/sections/" + second.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sortOrder\":1}"))
				.andExpect(status().isOk());

		MvcResult list = mockMvc.perform(get("/api/courses/" + course.getId() + "/sections"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode nodes = objectMapper.readTree(list.getResponse().getContentAsString());
		assertThat(nodes.get(0).get("title").asText()).isEqualTo("Second");
		assertThat(nodes.get(1).get("title").asText()).isEqualTo("First");
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	private User seedUser(UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName("Seed");
		user.setRole(role);
		user.setActive(true);
		return userRepository.save(user);
	}

	private Course seedCourse(User instructor, boolean published) {
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle("Course-" + UUID.randomUUID());
		course.setInstructor(instructor);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(published);
		return courseRepository.save(course);
	}

	private Section seedSection(Course course, String title, int sortOrder) {
		Section section = new Section();
		section.setId(UUID.randomUUID());
		section.setTitle(title);
		section.setCourse(course);
		section.setSortOrder(sortOrder);
		return sectionRepository.save(section);
	}

	private Lesson seedLesson(Section section, String title, int sortOrder) {
		Lesson lesson = new Lesson();
		lesson.setId(UUID.randomUUID());
		lesson.setTitle(title);
		lesson.setSection(section);
		lesson.setLessonType(LessonType.ARTICLE);
		lesson.setSortOrder(sortOrder);
		return lessonRepository.save(lesson);
	}
}
