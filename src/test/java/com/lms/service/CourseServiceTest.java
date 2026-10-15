package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lms.dto.course.CourseResponse;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.CourseMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.LessonRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private UserService userService;

	@Mock
	private LessonRepository lessonRepository;

	private CourseService courseService;

	@BeforeEach
	void setUp() {
		courseService = new CourseService(
				courseRepository,
				lessonRepository,
				new CourseMapper(),
				userService);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void create_rejectsPaidPriceInMvp() {
		UUID instructorId = UUID.randomUUID();
		CreateCourseRequest request = new CreateCourseRequest(
				"Paid Course",
				"desc",
				instructorId,
				new BigDecimal("9.99"),
				null,
				false);

		assertThatThrownBy(() -> courseService.create(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Only free courses");
	}

	@Test
	void create_rejectsNegativePrice() {
		CreateCourseRequest request = new CreateCourseRequest(
				"Course",
				null,
				UUID.randomUUID(),
				new BigDecimal("-1"),
				null,
				false);

		assertThatThrownBy(() -> courseService.create(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("non-negative");
	}

	@Test
	void create_rejectsStudentAsInstructor() {
		UUID instructorId = UUID.randomUUID();
		User student = instructor(instructorId, UserRole.STUDENT);
		when(userService.requireUser(instructorId)).thenReturn(student);

		CreateCourseRequest request = new CreateCourseRequest(
				"Course",
				null,
				instructorId,
				BigDecimal.ZERO,
				null,
				false);

		assertThatThrownBy(() -> courseService.create(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("INSTRUCTOR or ADMIN");
	}

	@Test
	void create_persistsFreeCourseForInstructor() {
		UUID instructorId = UUID.randomUUID();
		User instructor = instructor(instructorId, UserRole.INSTRUCTOR);
		when(userService.requireUser(instructorId)).thenReturn(instructor);
		when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CourseResponse response = courseService.create(new CreateCourseRequest(
				" Intro to LMS ",
				"  Overview  ",
				instructorId,
				BigDecimal.ZERO,
				" https://cdn/thumb.png ",
				true));

		ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
		verify(courseRepository).save(captor.capture());
		Course saved = captor.getValue();

		assertThat(saved.getTitle()).isEqualTo("Intro to LMS");
		assertThat(saved.getDescription()).isEqualTo("Overview");
		assertThat(saved.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(saved.isPublished()).isTrue();
		assertThat(saved.getThumbnailUrl()).isEqualTo("https://cdn/thumb.png");
		assertThat(response.instructorId()).isEqualTo(instructorId);
		assertThat(response.thumbnailUrl()).isEqualTo("https://cdn/thumb.png");
		assertThat(response.lessonCount()).isZero();
		assertThat(response.finalQuizId()).isNull();
	}

	@Test
	void getById_throwsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(courseRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> courseService.getById(id))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void getById_hidesUnpublishedFromNonAdmin() {
		Course course = sampleCourse(false);
		when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> courseService.getById(course.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void getById_allowsUnpublishedForAdmin() {
		asAdmin();
		Course course = sampleCourse(false);
		when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
		when(lessonRepository.countBySection_Course_Id(course.getId())).thenReturn(3L);

		CourseResponse response = courseService.getById(course.getId());

		assertThat(response.published()).isFalse();
		assertThat(response.title()).isEqualTo("Course");
		assertThat(response.instructorName()).isEqualTo("Instructor");
		assertThat(response.lessonCount()).isEqualTo(3L);
	}

	@Test
	void list_forcesPublishedOnlyForNonAdmin() {
		Course published = sampleCourse(true);
		when(courseRepository.findByPublished(true)).thenReturn(List.of(published));
		when(lessonRepository.countLessonsGroupedByCourseId(anyCollection())).thenReturn(List.of());

		List<CourseResponse> responses = courseService.list(false, null);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).published()).isTrue();
		verify(courseRepository).findByPublished(true);
	}

	@Test
	void list_allowsUnpublishedFilterForAdmin() {
		asAdmin();
		Course unpublished = sampleCourse(false);
		when(courseRepository.findByPublished(false)).thenReturn(List.of(unpublished));
		when(lessonRepository.countLessonsGroupedByCourseId(anyCollection())).thenReturn(List.of());

		List<CourseResponse> responses = courseService.list(false, null);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).published()).isFalse();
	}

	@Test
	void unpublish_setsPublishedFalse() {
		UUID id = UUID.randomUUID();
		User instructor = instructor(UUID.randomUUID(), UserRole.INSTRUCTOR);
		Course course = new Course();
		course.setId(id);
		course.setTitle("Course");
		course.setInstructor(instructor);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(true);

		when(courseRepository.findById(id)).thenReturn(Optional.of(course));
		when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(lessonRepository.countBySection_Course_Id(id)).thenReturn(0L);

		CourseResponse response = courseService.unpublish(id);

		assertThat(response.published()).isFalse();
	}

	@Test
	void publish_setsPublishedTrue() {
		Course course = sampleCourse(false);
		when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
		when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(lessonRepository.countBySection_Course_Id(course.getId())).thenReturn(0L);

		CourseResponse response = courseService.publish(course.getId());

		assertThat(response.published()).isTrue();
	}

	@Test
	void update_rejectsPaidPrice() {
		UUID id = UUID.randomUUID();
		User instructor = instructor(UUID.randomUUID(), UserRole.INSTRUCTOR);
		Course course = new Course();
		course.setId(id);
		course.setTitle("Course");
		course.setInstructor(instructor);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(false);

		when(courseRepository.findById(id)).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> courseService.update(
				id,
				new UpdateCourseRequest(null, null, new BigDecimal("1.00"), null, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Only free courses");
	}

	private static void asAdmin() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						"admin",
						"n/a",
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
	}

	private static Course sampleCourse(boolean published) {
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle("Course");
		course.setInstructor(instructor(UUID.randomUUID(), UserRole.INSTRUCTOR));
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(published);
		return course;
	}

	private static User instructor(UUID id, UserRole role) {
		User user = new User();
		user.setId(id);
		user.setEmail("instructor@example.com");
		user.setPasswordHash("hash");
		user.setFullName("Instructor");
		user.setRole(role);
		user.setActive(true);
		return user;
	}
}
