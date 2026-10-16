package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
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
import com.lms.dto.progress.UpsertLessonProgressRequest;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.LessonProgressRepository;
import com.lms.repository.LessonRepository;
import com.lms.repository.SectionRepository;
import com.lms.repository.UserRepository;
import com.lms.exception.ConflictException;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.LessonProgressService;

@SpringBootTest
class LessonProgressIntegrationTest {

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
	private LessonProgressRepository lessonProgressRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private LessonProgressService lessonProgressService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void progress_readsRequireAuthentication() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();

		mockMvc.perform(get(progressListUrl(fixture.course)))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(lessonProgressUrl(fixture.course, fixture.lesson)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void progress_writesRequireAuthentication() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();

		mockMvc.perform(put(lessonProgressUrl(fixture.course, fixture.lesson))
						.contentType(APPLICATION_JSON)
						.content("{\"watchedDurationSeconds\":10}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post(completeUrl(fixture.course, fixture.lesson)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void studentWithoutEnrollment_cannotAccessProgress() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);

		mockMvc.perform(get(progressListUrl(fixture.course))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(lessonProgressUrl(fixture.course, fixture.lesson))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isForbidden());
	}

	@Test
	void lessonNotInCourse_returns404() throws Exception {
		Fixture target = seedPublishedCourseWithVideoLesson();
		Fixture other = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, target.course, EnrollmentStatus.ACTIVE);

		mockMvc.perform(get(lessonProgressUrl(target.course, other.lesson))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isNotFound());
	}

	@Test
	void unstartedLesson_returnsDefaultProgressWithoutPersisting() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.ACTIVE);

		mockMvc.perform(get(lessonProgressUrl(fixture.course, fixture.lesson))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lessonId").value(fixture.lesson.getId().toString()))
				.andExpect(jsonPath("$.isCompleted").value(false))
				.andExpect(jsonPath("$.watchedDurationSeconds").value(0))
				.andExpect(jsonPath("$.lastAccessedAt").isEmpty());

		assertThat(lessonProgressRepository.findByUserIdAndLessonId(
				student.getId(), fixture.lesson.getId())).isEmpty();
	}

	@Test
	void upsert_createsAndUpdatesProgress() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.ACTIVE);
		String url = lessonProgressUrl(fixture.course, fixture.lesson);

		mockMvc.perform(put(url)
						.header(HttpHeaders.AUTHORIZATION, bearer(student))
						.contentType(APPLICATION_JSON)
						.content("{\"watchedDurationSeconds\":30}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isCompleted").value(false))
				.andExpect(jsonPath("$.watchedDurationSeconds").value(30))
				.andExpect(jsonPath("$.lastAccessedAt").isNotEmpty());

		mockMvc.perform(put(url)
						.header(HttpHeaders.AUTHORIZATION, bearer(student))
						.contentType(APPLICATION_JSON)
						.content("{\"watchedDurationSeconds\":55}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.watchedDurationSeconds").value(55));

		mockMvc.perform(get(progressListUrl(fixture.course))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].lessonId").value(fixture.lesson.getId().toString()));
	}

	@Test
	void upsert_rejectsNegativeWatchedDuration() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.ACTIVE);

		mockMvc.perform(put(lessonProgressUrl(fixture.course, fixture.lesson))
						.header(HttpHeaders.AUTHORIZATION, bearer(student))
						.contentType(APPLICATION_JSON)
						.content("{\"watchedDurationSeconds\":-1}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void complete_isIdempotent_andPutDoesNotClearCompletion() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.ACTIVE);
		String progressUrl = lessonProgressUrl(fixture.course, fixture.lesson);
		String complete = completeUrl(fixture.course, fixture.lesson);

		mockMvc.perform(post(complete)
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isCompleted").value(true));

		mockMvc.perform(post(complete)
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isCompleted").value(true));

		mockMvc.perform(put(progressUrl)
						.header(HttpHeaders.AUTHORIZATION, bearer(student))
						.contentType(APPLICATION_JSON)
						.content("{\"watchedDurationSeconds\":12}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isCompleted").value(true))
				.andExpect(jsonPath("$.watchedDurationSeconds").value(12));
	}

	@Test
	void complete_rejectsQuizAndAssignmentLessons() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.ACTIVE);

		Lesson quiz = seedLesson(fixture.section, "Quiz", 1, LessonType.QUIZ, null);
		Lesson assignment = seedLesson(fixture.section, "Assign", 2, LessonType.ASSIGNMENT, null);

		mockMvc.perform(post(completeUrl(fixture.course, quiz))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Quiz lessons cannot be marked complete via this endpoint"));

		mockMvc.perform(post(completeUrl(fixture.course, assignment))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Assignment lessons cannot be marked complete via this endpoint"));
	}

	@Test
	void unpublishedCourse_returns404ForEnrolledStudent() throws Exception {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		User student = seedUser(UserRole.STUDENT);
		Course draft = seedCourse(instructor, false);
		Section section = seedSection(draft, "S", 0);
		Lesson lesson = seedLesson(section, "L", 0, LessonType.VIDEO, "https://cdn.example/v.mp4");
		seedEnrollment(student, draft, EnrollmentStatus.ACTIVE);

		mockMvc.perform(get(lessonProgressUrl(draft, lesson))
						.header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isNotFound());
	}

	@Test
	void progress_isolatedPerLearner() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User studentA = seedUser(UserRole.STUDENT);
		User studentB = seedUser(UserRole.STUDENT);
		seedEnrollment(studentA, fixture.course, EnrollmentStatus.ACTIVE);
		seedEnrollment(studentB, fixture.course, EnrollmentStatus.ACTIVE);
		String url = lessonProgressUrl(fixture.course, fixture.lesson);

		mockMvc.perform(put(url)
						.header(HttpHeaders.AUTHORIZATION, bearer(studentA))
						.contentType(APPLICATION_JSON)
						.content("{\"watchedDurationSeconds\":40}"))
				.andExpect(status().isOk());

		mockMvc.perform(get(url)
						.header(HttpHeaders.AUTHORIZATION, bearer(studentB)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.watchedDurationSeconds").value(0))
				.andExpect(jsonPath("$.isCompleted").value(false));
	}

	@Test
	void upsert_concurrentFirstWrites_onlyOneRowPersists() throws Exception {
		Fixture fixture = seedPublishedCourseWithVideoLesson();
		User student = seedUser(UserRole.STUDENT);
		seedEnrollment(student, fixture.course, EnrollmentStatus.ACTIVE);
		UUID courseId = fixture.course.getId();
		UUID lessonId = fixture.lesson.getId();
		UUID userId = student.getId();
		var request = new UpsertLessonProgressRequest(77);

		int threads = 8;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger created = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();

		try {
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> {
					ready.countDown();
					start.await(5, TimeUnit.SECONDS);
					authenticateAs(student);
					try {
						lessonProgressService.upsert(courseId, lessonId, userId, request);
						created.incrementAndGet();
					}
					catch (ConflictException ex) {
						conflicts.incrementAndGet();
					}
					finally {
						SecurityContextHolder.clearContext();
					}
					return null;
				}));
			}
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<?> future : futures) {
				future.get(15, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(created.get() + conflicts.get()).isEqualTo(threads);
		assertThat(created.get()).isGreaterThanOrEqualTo(1);
		assertThat(lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)).isPresent();
		assertThat(lessonProgressRepository.findByUserId(userId).stream()
				.filter(progress -> progress.getLesson().getId().equals(lessonId))
				.count()).isEqualTo(1L);
	}

	private static void authenticateAs(User user) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(
				new LmsUserPrincipal(user), null, new LmsUserPrincipal(user).getAuthorities()));
		SecurityContextHolder.setContext(context);
	}

	private static String progressListUrl(Course course) {
		return "/api/courses/" + course.getId() + "/progress";
	}

	private static String lessonProgressUrl(Course course, Lesson lesson) {
		return "/api/courses/" + course.getId() + "/lessons/" + lesson.getId() + "/progress";
	}

	private static String completeUrl(Course course, Lesson lesson) {
		return lessonProgressUrl(course, lesson) + "/complete";
	}

	private String bearer(User user) {
		return "Bearer " + jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	private Fixture seedPublishedCourseWithVideoLesson() {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		Course course = seedCourse(instructor, true);
		Section section = seedSection(course, "Week 1", 0);
		Lesson lesson = seedLesson(section, "Intro", 0, LessonType.VIDEO, "https://cdn.example/v.mp4");
		return new Fixture(course, section, lesson);
	}

	private User seedUser(UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName("Progress Tester");
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

	private Lesson seedLesson(
			Section section,
			String title,
			int sortOrder,
			LessonType type,
			String contentUrl) {
		Lesson lesson = new Lesson();
		lesson.setId(UUID.randomUUID());
		lesson.setTitle(title);
		lesson.setSection(section);
		lesson.setLessonType(type);
		lesson.setContentUrl(contentUrl);
		lesson.setDurationSeconds(type == LessonType.VIDEO ? 90 : null);
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
	}
}
