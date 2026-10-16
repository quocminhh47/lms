package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.EnrollmentStatus;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Section;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.LessonRepository;
import com.lms.repository.SectionRepository;
import com.lms.repository.UserRepository;

@SpringBootTest
class LearningContentAccessIntegrationTest {

	private static final String SECRET_URL = "https://cdn.example/protected-video.mp4";

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
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void guestCannotAccessProtectedLessonContent_butCanReadCurriculumMetadata() throws Exception {
		Fixture fixture = seedPublishedCourseWithLesson();

		mockMvc.perform(get("/api/courses/" + fixture.course.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value(fixture.course.getTitle()));

		mockMvc.perform(get("/api/courses/" + fixture.course.getId() + "/sections"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Week 1"));

		MvcResult list = mockMvc.perform(get(fixture.lessonsUrl()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Intro"))
				.andReturn();
		assertThat(list.getResponse().getContentAsString()).doesNotContain(SECRET_URL);

		mockMvc.perform(get(fixture.lessonUrl()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/lessons/" + fixture.lesson.getId()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void studentWithoutEnrollment_cannotAccessLessonContent() throws Exception {
		Fixture fixture = seedPublishedCourseWithLesson();
		User student = seedUser(UserRole.STUDENT);

		mockMvc.perform(get(fixture.lessonUrl())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/lessons/" + fixture.lesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isForbidden());
	}

	@Test
	void activeEnrollment_grantsLessonContent() throws Exception {
		Fixture fixture = seedPublishedCourseWithLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.ACTIVE);

		mockMvc.perform(get(fixture.lessonUrl())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentUrl").value(SECRET_URL))
				.andExpect(jsonPath("$.title").value("Intro"));

		mockMvc.perform(get("/api/lessons/" + fixture.lesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentUrl").value(SECRET_URL));
	}

	@Test
	void completedEnrollment_grantsLessonContent() throws Exception {
		Fixture fixture = seedPublishedCourseWithLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.COMPLETED);

		mockMvc.perform(get(fixture.lessonUrl())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentUrl").value(SECRET_URL));
	}

	@Test
	void pendingAndDroppedEnrollment_doNotGrantLessonContent() throws Exception {
		Fixture fixture = seedPublishedCourseWithLesson();
		User pendingStudent = seedUser(UserRole.STUDENT);
		User droppedStudent = seedUser(UserRole.STUDENT);
		seedEnrollment(pendingStudent, fixture.course, EnrollmentStatus.PENDING);
		seedEnrollment(droppedStudent, fixture.course, EnrollmentStatus.DROPPED);

		mockMvc.perform(get(fixture.lessonUrl())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(pendingStudent)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(fixture.lessonUrl())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(droppedStudent)))
				.andExpect(status().isForbidden());
	}

	@Test
	void enrollmentInAnotherCourse_doesNotGrantAccess() throws Exception {
		Fixture target = seedPublishedCourseWithLesson();
		Fixture other = seedPublishedCourseWithLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, other.course, EnrollmentStatus.ACTIVE);

		mockMvc.perform(get(target.lessonUrl())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanAccessPublishedAndUnpublishedLessonContent() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User admin = seedUser(UserRole.ADMIN);
		Course published = seedCourse(instructor, true);
		Course draft = seedCourse(instructor, false);
		Section pubSection = seedSection(published, "S", 0);
		Section draftSection = seedSection(draft, "D", 0);
		Lesson pubLesson = seedLesson(pubSection, "P", 0, SECRET_URL);
		Lesson draftLesson = seedLesson(draftSection, "Draft L", 0, SECRET_URL);

		mockMvc.perform(get("/api/lessons/" + pubLesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentUrl").value(SECRET_URL));

		mockMvc.perform(get("/api/lessons/" + draftLesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentUrl").value(SECRET_URL));
	}

	@Test
	void unpublishedLesson_still404ForNonAdminEvenIfEnrolled() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User student = seedUser(UserRole.STUDENT);
		Course draft = seedCourse(instructor, false);
		Section section = seedSection(draft, "S", 0);
		Lesson lesson = seedLesson(section, "L", 0, SECRET_URL);
		seedEnrollment(student, draft, EnrollmentStatus.ACTIVE);

		mockMvc.perform(get("/api/lessons/" + lesson.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isNotFound());
	}

	private Fixture seedPublishedCourseWithLesson() {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		Course course = seedCourse(instructor, true);
		Section section = seedSection(course, "Week 1", 0);
		Lesson lesson = seedLesson(section, "Intro", 0, SECRET_URL);
		return new Fixture(course, section, lesson);
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	private User seedUser(UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName("Learner");
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

	private Lesson seedLesson(Section section, String title, int sortOrder, String contentUrl) {
		Lesson lesson = new Lesson();
		lesson.setId(UUID.randomUUID());
		lesson.setTitle(title);
		lesson.setSection(section);
		lesson.setLessonType(LessonType.VIDEO);
		lesson.setContentUrl(contentUrl);
		lesson.setDurationSeconds(90);
		lesson.setSortOrder(sortOrder);
		return lessonRepository.save(lesson);
	}

	private void seedEnrollment(User user, Course course, EnrollmentStatus status) {
		Enrollment enrollment = new Enrollment();
		enrollment.setId(UUID.randomUUID());
		enrollment.setUser(user);
		enrollment.setCourse(course);
		enrollment.setStatus(status);
		enrollmentRepository.save(enrollment);
	}

	private record Fixture(Course course, Section section, Lesson lesson) {
		String lessonsUrl() {
			return "/api/courses/" + course.getId() + "/sections/" + section.getId() + "/lessons";
		}

		String lessonUrl() {
			return lessonsUrl() + "/" + lesson.getId();
		}
	}
}
