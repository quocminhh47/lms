package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.EnrollmentStatus;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.EnrollmentMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private UserService userService;

	private EnrollmentService enrollmentService;

	@BeforeEach
	void setUp() {
		enrollmentService = new EnrollmentService(
				enrollmentRepository,
				courseRepository,
				userService,
				new EnrollmentMapper());
	}

	@Test
	void enroll_createsActiveEnrollmentForPublishedFreeCourse() {
		UUID userId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		User student = user(userId);
		Course course = publishedFreeCourse(courseId, "HSK 1");

		when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
		when(userService.requireUser(userId)).thenReturn(student);
		when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)).thenReturn(Optional.empty());
		when(enrollmentRepository.saveAndFlush(any(Enrollment.class))).thenAnswer(invocation -> {
			Enrollment saved = invocation.getArgument(0);
			saved.setStatus(EnrollmentStatus.ACTIVE);
			return saved;
		});

		EnrollmentResponse response = enrollmentService.enroll(courseId, userId);

		ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
		verify(enrollmentRepository).saveAndFlush(captor.capture());

		assertThat(captor.getValue().getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
		assertThat(captor.getValue().getUser()).isEqualTo(student);
		assertThat(captor.getValue().getCourse()).isEqualTo(course);
		assertThat(response.courseId()).isEqualTo(courseId);
		assertThat(response.courseTitle()).isEqualTo("HSK 1");
		assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
		assertThat(response.userId()).isEqualTo(userId);
	}

	@Test
	void enroll_missingCourse_returnsNotFound() {
		UUID courseId = UUID.randomUUID();
		when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enrollmentService.enroll(courseId, UUID.randomUUID()))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(courseId.toString());
		verify(enrollmentRepository, never()).saveAndFlush(any());
	}

	@Test
	void enroll_unpublishedCourse_returnsNotFound() {
		UUID courseId = UUID.randomUUID();
		Course course = publishedFreeCourse(courseId, "Draft");
		course.setPublished(false);
		when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> enrollmentService.enroll(courseId, UUID.randomUUID()))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(enrollmentRepository, never()).saveAndFlush(any());
	}

	@Test
	void enroll_paidCourse_rejected() {
		UUID userId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		Course course = publishedFreeCourse(courseId, "Paid Demo");
		course.setPrice(new BigDecimal("49.99"));
		when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> enrollmentService.enroll(courseId, userId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Only free courses");
		verify(enrollmentRepository, never()).saveAndFlush(any());
	}

	@Test
	void enroll_duplicate_returnsConflict() {
		UUID userId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		Course course = publishedFreeCourse(courseId, "HSK 1");
		Enrollment existing = new Enrollment();
		existing.setId(UUID.randomUUID());

		when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
		when(userService.requireUser(userId)).thenReturn(user(userId));
		when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
				.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> enrollmentService.enroll(courseId, userId))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("Already enrolled");
		verify(enrollmentRepository, never()).saveAndFlush(any());
	}

	@Test
	void listMine_returnsOnlyMappedRows() {
		UUID userId = UUID.randomUUID();
		Enrollment enrollment = ownedEnrollment(userId, "Course A");
		when(enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId))
				.thenReturn(List.of(enrollment));

		List<EnrollmentResponse> responses = enrollmentService.listMine(userId);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).courseTitle()).isEqualTo("Course A");
		assertThat(responses.get(0).userId()).isEqualTo(userId);
	}

	@Test
	void getMine_missingOrForeign_returnsNotFound() {
		UUID enrollmentId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enrollmentService.getMine(enrollmentId, userId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(enrollmentId.toString());
	}

	private static User user(UUID id) {
		User user = new User();
		user.setId(id);
		user.setEmail(id + "@example.com");
		user.setPasswordHash("hash");
		user.setFullName("Student");
		user.setRole(UserRole.STUDENT);
		user.setActive(true);
		return user;
	}

	private static Course publishedFreeCourse(UUID id, String title) {
		Course course = new Course();
		course.setId(id);
		course.setTitle(title);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(true);
		User instructor = user(UUID.randomUUID());
		instructor.setRole(UserRole.INSTRUCTOR);
		course.setInstructor(instructor);
		return course;
	}

	private static Enrollment ownedEnrollment(UUID userId, String title) {
		Enrollment enrollment = new Enrollment();
		enrollment.setId(UUID.randomUUID());
		enrollment.setUser(user(userId));
		enrollment.setCourse(publishedFreeCourse(UUID.randomUUID(), title));
		enrollment.setStatus(EnrollmentStatus.ACTIVE);
		return enrollment;
	}
}
