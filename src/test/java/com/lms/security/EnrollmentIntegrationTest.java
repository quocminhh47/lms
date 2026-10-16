package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.EnrollmentStatus;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import com.lms.service.EnrollmentService;

@SpringBootTest
class EnrollmentIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private EnrollmentService enrollmentService;

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
	void enroll_requiresAuthentication() throws Exception {
		Course course = seedPublishedFreeCourse("Auth Required Course");

		mockMvc.perform(post("/api/courses/" + course.getId() + "/enrollments"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void enroll_publishedFreeCourse_createsActiveEnrollment() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		Course course = seedPublishedFreeCourse("Active Enroll Course");

		mockMvc.perform(post("/api/courses/" + course.getId() + "/enrollments")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(student.getId().toString()))
				.andExpect(jsonPath("$.courseId").value(course.getId().toString()))
				.andExpect(jsonPath("$.courseTitle").value("Active Enroll Course"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.enrolledAt").isNotEmpty())
				.andExpect(jsonPath("$.id").isNotEmpty());

		Enrollment stored = enrollmentRepository
				.findByUserIdAndCourseId(student.getId(), course.getId())
				.orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
	}

	@Test
	void enroll_missingCourse_returns404() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		UUID missing = UUID.randomUUID();

		mockMvc.perform(post("/api/courses/" + missing + "/enrollments")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isNotFound());
	}

	@Test
	void enroll_unpublishedCourse_returns404ForStudent() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		Course course = seedCourse("Unpublished Enroll", false, BigDecimal.ZERO);

		mockMvc.perform(post("/api/courses/" + course.getId() + "/enrollments")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isNotFound());
	}

	@Test
	void enroll_paidCourse_returns400() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		Course course = seedCourse("Paid Legacy Course", true, new BigDecimal("49.99"));

		String message = mockMvc.perform(post("/api/courses/" + course.getId() + "/enrollments")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student)))
				.andExpect(status().isBadRequest())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(message).containsIgnoringCase("free");
	}

	@Test
	void enroll_duplicate_returns409() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		Course course = seedPublishedFreeCourse("Duplicate Enroll Course");
		String token = tokenFor(student);

		mockMvc.perform(post("/api/courses/" + course.getId() + "/enrollments")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/courses/" + course.getId() + "/enrollments")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Already enrolled in this course"));
	}

	@Test
	void enroll_concurrentDuplicates_onlyOneRowPersists() throws Exception {
		User student = seedUser(UserRole.STUDENT);
		Course course = seedPublishedFreeCourse("Concurrent Enroll Course");
		UUID userId = student.getId();
		UUID courseId = course.getId();

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
					try {
						enrollmentService.enroll(courseId, userId);
						created.incrementAndGet();
					}
					catch (com.lms.exception.ConflictException ex) {
						conflicts.incrementAndGet();
					}
					return null;
				}));
			}
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<?> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(created.get()).isEqualTo(1);
		assertThat(conflicts.get()).isEqualTo(threads - 1);
		assertThat(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)).isPresent();
		assertThat(enrollmentRepository.findByUserId(userId).stream()
				.filter(enrollment -> enrollment.getCourse().getId().equals(courseId))
				.count()).isEqualTo(1L);
	}

	@Test
	void listMine_returnsOnlyCallerEnrollments_andEmptyWhenNone() throws Exception {
		User studentA = seedUser(UserRole.STUDENT);
		User studentB = seedUser(UserRole.STUDENT);
		Course course = seedPublishedFreeCourse("Mine List Course");

		mockMvc.perform(get("/api/enrollments/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(studentA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));

		enrollmentService.enroll(course.getId(), studentA.getId());
		enrollmentService.enroll(course.getId(), studentB.getId());

		mockMvc.perform(get("/api/enrollments/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(studentA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].userId").value(studentA.getId().toString()))
				.andExpect(jsonPath("$[0].courseTitle").value("Mine List Course"));
	}

	@Test
	void getMine_ownerOk_otherUser404() throws Exception {
		User owner = seedUser(UserRole.STUDENT);
		User other = seedUser(UserRole.STUDENT);
		Course course = seedPublishedFreeCourse("Ownership Course");
		var response = enrollmentService.enroll(course.getId(), owner.getId());

		mockMvc.perform(get("/api/enrollments/me/" + response.id())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(response.id().toString()))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(get("/api/enrollments/me/" + response.id())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(other)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/enrollments/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(other)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void listMine_requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/enrollments/me"))
				.andExpect(status().isUnauthorized());
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	private User seedUser(UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName("Enrollment Tester");
		user.setRole(role);
		user.setActive(true);
		return userRepository.save(user);
	}

	private Course seedPublishedFreeCourse(String title) {
		return seedCourse(title, true, BigDecimal.ZERO);
	}

	private Course seedCourse(String title, boolean published, BigDecimal price) {
		User instructor = seedUser(UserRole.INSTRUCTOR);
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle(title);
		course.setDescription("Enrollment test course");
		course.setInstructor(instructor);
		course.setPrice(price);
		course.setPublished(published);
		return courseRepository.save(course);
	}
}
