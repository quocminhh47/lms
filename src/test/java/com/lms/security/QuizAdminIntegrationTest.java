package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class QuizAdminIntegrationTest {

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
	void adminCanUpsertAndGetQuiz_studentForbidden() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User student = seedUser(UserRole.STUDENT);
		User admin = seedUser(UserRole.ADMIN);
		Course course = seedCourse(instructor);
		Lesson lesson = seedQuizLesson(course);

		String url = "/api/courses/" + course.getId() + "/lessons/" + lesson.getId() + "/quiz";
		String body = """
				{
				  "passingScore": 80,
				  "timeLimitMinutes": 15,
				  "maxAttempts": 3,
				  "questions": [
				    {
				      "questionText": "2 + 2 = ?",
				      "questionType": "SINGLE_CHOICE",
				      "scoreWeight": 1,
				      "options": [
				        {"optionText": "4", "isCorrect": true},
				        {"optionText": "5", "isCorrect": false}
				      ]
				    }
				  ]
				}
				""";

		mockMvc.perform(get(url)).andExpect(status().isForbidden());

		mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isForbidden());
		mockMvc.perform(put(url)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());

		MvcResult created = mockMvc.perform(put(url)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passingScore").value(80))
				.andExpect(jsonPath("$.lessonId").value(lesson.getId().toString()))
				.andExpect(jsonPath("$.courseId").value(course.getId().toString()))
				.andExpect(jsonPath("$.questions[0].questionText").value("2 + 2 = ?"))
				.andExpect(jsonPath("$.questions[0].options[0].isCorrect").value(true))
				.andExpect(jsonPath("$.finalAssessment").value(false))
				.andReturn();

		JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
		assertThat(json.get("quizId").asText()).isNotBlank();

		mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions.length()").value(1));
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

	private Course seedCourse(User instructor) {
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle("Course");
		course.setInstructor(instructor);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(true);
		return courseRepository.save(course);
	}

	private Lesson seedQuizLesson(Course course) {
		Section section = new Section();
		section.setId(UUID.randomUUID());
		section.setCourse(course);
		section.setTitle("Section");
		section.setSortOrder(0);
		section = sectionRepository.save(section);

		Lesson lesson = new Lesson();
		lesson.setId(UUID.randomUUID());
		lesson.setSection(section);
		lesson.setTitle("Quiz");
		lesson.setLessonType(LessonType.QUIZ);
		lesson.setSortOrder(0);
		return lessonRepository.save(lesson);
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}
}
