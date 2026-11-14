package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.EnrollmentStatus;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Question;
import com.lms.entity.QuestionOption;
import com.lms.entity.QuestionType;
import com.lms.entity.Quiz;
import com.lms.entity.QuizAttempt;
import com.lms.entity.QuizAttemptStatus;
import com.lms.entity.Section;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.LessonRepository;
import com.lms.repository.QuestionOptionRepository;
import com.lms.repository.QuestionRepository;
import com.lms.repository.QuizAttemptRepository;
import com.lms.repository.QuizRepository;
import com.lms.repository.SectionRepository;
import com.lms.repository.UserRepository;

@SpringBootTest
class QuizAttemptIntegrationTest {

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
	private QuizRepository quizRepository;
	@Autowired
	private QuestionRepository questionRepository;
	@Autowired
	private QuestionOptionRepository questionOptionRepository;
	@Autowired
	private EnrollmentRepository enrollmentRepository;
	@Autowired
	private QuizAttemptRepository quizAttemptRepository;
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
	void studentStartsSubmitsAndPassesSectionQuiz() throws Exception {
		Fixture fixture = seedQuizCourse(100, null, false);
		enroll(fixture.student(), fixture.course());

		String base = attemptsUrl(fixture);
		String studentToken = tokenFor(fixture.student());

		mockMvc.perform(get(base + "/session").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isNotFound());

		MvcResult started = mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.passingScore").value(100))
				.andExpect(jsonPath("$.questions.length()").value(2))
				.andReturn();

		assertThat(started.getResponse().getContentAsString()).doesNotContain("isCorrect");

		JsonNode startJson = objectMapper.readTree(started.getResponse().getContentAsString());
		String attemptId = startJson.get("attemptId").asText();
		String q1 = startJson.get("questions").get(0).get("id").asText();
		String correct1 = startJson.get("questions").get(0).get("options").get(0).get("id").asText();
		String q2 = startJson.get("questions").get(1).get("id").asText();
		String correct2a = startJson.get("questions").get(1).get("options").get(0).get("id").asText();
		String correct2b = startJson.get("questions").get(1).get("options").get(1).get("id").asText();

		String submitBody = """
				{
				  "answers": [
				    {"questionId": "%s", "selectedOptionIds": ["%s"]},
				    {"questionId": "%s", "selectedOptionIds": ["%s", "%s"]}
				  ]
				}
				""".formatted(q1, correct1, q2, correct2a, correct2b);

		mockMvc.perform(post(base + "/" + attemptId + "/submit")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.score").value(100.0))
				.andExpect(jsonPath("$.passed").value(true))
				.andExpect(jsonPath("$.bestScore").value(100.0))
				.andExpect(jsonPath("$.bestPassed").value(true));

		mockMvc.perform(get(base + "/session").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"));
	}

	@Test
	void multipleChoiceRequiresExactOptionSet() throws Exception {
		Fixture fixture = seedQuizCourse(100, null, false);
		enroll(fixture.student(), fixture.course());
		String base = attemptsUrl(fixture);
		String studentToken = tokenFor(fixture.student());

		MvcResult started = mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode startJson = objectMapper.readTree(started.getResponse().getContentAsString());
		String attemptId = startJson.get("attemptId").asText();
		String q1 = startJson.get("questions").get(0).get("id").asText();
		String correct1 = startJson.get("questions").get(0).get("options").get(0).get("id").asText();
		String q2 = startJson.get("questions").get(1).get("id").asText();
		String correct2a = startJson.get("questions").get(1).get("options").get(0).get("id").asText();

		String partialMulti = """
				{
				  "answers": [
				    {"questionId": "%s", "selectedOptionIds": ["%s"]},
				    {"questionId": "%s", "selectedOptionIds": ["%s"]}
				  ]
				}
				""".formatted(q1, correct1, q2, correct2a);

		mockMvc.perform(post(base + "/" + attemptId + "/submit")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(partialMulti))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passed").value(false))
				.andExpect(jsonPath("$.score").value(50.0));
	}

	@Test
	void expiredInProgressAttemptCannotResume_newAttemptCanStart() throws Exception {
		Fixture fixture = seedQuizCourse(100, 1, false);
		enroll(fixture.student(), fixture.course());
		String base = attemptsUrl(fixture);
		String studentToken = tokenFor(fixture.student());

		MvcResult started = mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isCreated())
				.andReturn();
		UUID attemptId = UUID.fromString(
				objectMapper.readTree(started.getResponse().getContentAsString()).get("attemptId").asText());

		QuizAttempt attempt = quizAttemptRepository.findById(attemptId).orElseThrow();
		attempt.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
		quizAttemptRepository.save(attempt);

		mockMvc.perform(get(base + "/current").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isNotFound());

		MvcResult resumed = mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isCreated())
				.andReturn();
		String newAttemptId = objectMapper.readTree(resumed.getResponse().getContentAsString())
				.get("attemptId").asText();
		assertThat(newAttemptId).isNotEqualTo(attemptId.toString());

		assertThat(quizAttemptRepository.findById(attemptId).orElseThrow().getStatus())
				.isEqualTo(QuizAttemptStatus.EXPIRED);
	}

	@Test
	void maxAttemptsEnforced() throws Exception {
		Fixture fixture = seedQuizCourse(80, 1, false);
		enroll(fixture.student(), fixture.course());
		String base = attemptsUrl(fixture);
		String studentToken = tokenFor(fixture.student());

		MvcResult started = mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode json = objectMapper.readTree(started.getResponse().getContentAsString());
		String attemptId = json.get("attemptId").asText();
		String q1 = json.get("questions").get(0).get("id").asText();
		String o1 = json.get("questions").get(0).get("options").get(1).get("id").asText();
		String q2 = json.get("questions").get(1).get("id").asText();
		String o2 = json.get("questions").get(1).get("options").get(2).get("id").asText();

		String submitBody = """
				{
				  "answers": [
				    {"questionId": "%s", "selectedOptionIds": ["%s"]},
				    {"questionId": "%s", "selectedOptionIds": ["%s"]}
				  ]
				}
				""".formatted(q1, o1, q2, o2);

		mockMvc.perform(post(base + "/" + attemptId + "/submit")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitBody))
				.andExpect(status().isOk());

		mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Maximum quiz attempts reached"));
	}

	@Test
	void enrollmentRequired() throws Exception {
		Fixture fixture = seedQuizCourse(100, null, false);
		String base = attemptsUrl(fixture);
		String studentToken = tokenFor(fixture.student());

		mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentCannotAccessAnotherStudentsAttempt() throws Exception {
		Fixture fixture = seedQuizCourse(100, null, false);
		User other = seedUser(UserRole.STUDENT);
		enroll(fixture.student(), fixture.course());

		String base = attemptsUrl(fixture);
		String studentToken = tokenFor(fixture.student());
		String otherToken = tokenFor(other);

		MvcResult started = mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isCreated())
				.andReturn();
		String attemptId = objectMapper.readTree(started.getResponse().getContentAsString())
				.get("attemptId").asText();

		mockMvc.perform(get(base + "/" + attemptId).header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void finalAssessmentUsesConfiguredPassingScore() throws Exception {
		Fixture fixture = seedQuizCourse(80, null, true);
		enroll(fixture.student(), fixture.course());
		String base = attemptsUrl(fixture);
		String studentToken = tokenFor(fixture.student());

		mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.finalAssessment").value(true))
				.andExpect(jsonPath("$.passingScore").value(80));
	}

	private static String attemptsUrl(Fixture fixture) {
		return "/api/courses/" + fixture.course().getId() + "/lessons/" + fixture.lesson().getId() + "/quiz-attempts";
	}

	private void enroll(User student, Course course) {
		Enrollment enrollment = new Enrollment();
		enrollment.setId(UUID.randomUUID());
		enrollment.setUser(student);
		enrollment.setCourse(course);
		enrollment.setStatus(EnrollmentStatus.ACTIVE);
		enrollmentRepository.save(enrollment);
	}

	private Fixture seedQuizCourse(int passingScore, Integer maxAttempts, boolean finalAssessment) {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User student = seedUser(UserRole.STUDENT);
		Course course = seedCourse(instructor);
		Lesson lesson = seedQuizLesson(course);

		Quiz quiz = new Quiz();
		quiz.setId(UUID.randomUUID());
		quiz.setLesson(lesson);
		quiz.setPassingScore(passingScore);
		quiz.setMaxAttempts(maxAttempts);
		quiz = quizRepository.save(quiz);

		if (finalAssessment) {
			course.setFinalQuiz(quiz);
			courseRepository.save(course);
		}

		Question single = new Question();
		single.setId(UUID.randomUUID());
		single.setQuiz(quiz);
		single.setQuestionText("Pick one");
		single.setQuestionType(QuestionType.SINGLE_CHOICE);
		single.setScoreWeight(1);
		single.setSortOrder(0);
		single = questionRepository.save(single);

		QuestionOption singleCorrect = option(single, "A", true, 0);
		QuestionOption singleWrong = option(single, "B", false, 1);

		Question multi = new Question();
		multi.setId(UUID.randomUUID());
		multi.setQuiz(quiz);
		multi.setQuestionText("Pick many");
		multi.setQuestionType(QuestionType.MULTIPLE_CHOICE);
		multi.setScoreWeight(1);
		multi.setSortOrder(1);
		multi = questionRepository.save(multi);

		option(multi, "X", true, 0);
		option(multi, "Y", true, 1);
		option(multi, "Z", false, 2);

		return new Fixture(course, lesson, quiz, student, singleCorrect, singleWrong);
	}

	private QuestionOption option(Question question, String text, boolean correct, int sortOrder) {
		QuestionOption option = new QuestionOption();
		option.setId(UUID.randomUUID());
		option.setQuestion(question);
		option.setOptionText(text);
		option.setCorrect(correct);
		option.setSortOrder(sortOrder);
		return questionOptionRepository.save(option);
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
		lesson.setTitle("Quiz lesson");
		lesson.setLessonType(LessonType.QUIZ);
		lesson.setSortOrder(0);
		return lessonRepository.save(lesson);
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	private record Fixture(
			Course course,
			Lesson lesson,
			Quiz quiz,
			User student,
			QuestionOption singleCorrect,
			QuestionOption singleWrong) {
	}
}
