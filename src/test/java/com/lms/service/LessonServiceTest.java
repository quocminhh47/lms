package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lms.dto.lesson.CreateLessonRequest;
import com.lms.dto.lesson.LessonResponse;
import com.lms.dto.lesson.UpdateLessonRequest;
import com.lms.entity.Course;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Section;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.LessonMapper;
import com.lms.repository.LessonRepository;
import com.lms.service.LessonArticlePersistenceService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonServiceTest {

	private final AtomicReference<Lesson> lastSavedLesson = new AtomicReference<>();

	@Mock
	private LessonRepository lessonRepository;

	@Mock
	private SectionService sectionService;

	@Mock
	private EnrollmentService enrollmentService;

	@Mock
	private LessonArticlePersistenceService lessonArticlePersistenceService;

	private LessonService lessonService;

	@BeforeEach
	void setUp() {
		lessonService = new LessonService(
				lessonRepository,
				sectionService,
				enrollmentService,
				new LessonMapper(),
				lessonArticlePersistenceService);
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
			Lesson saved = invocation.getArgument(0);
			lastSavedLesson.set(saved);
			return saved;
		});
		when(lessonRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
			Lesson saved = lastSavedLesson.get();
			if (saved != null && saved.getId().equals(invocation.getArgument(0))) {
				return Optional.of(saved);
			}
			return Optional.empty();
		});
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void create_persistsLessonInSection() {
		Course course = course(true);
		Section section = section(course);
		when(sectionService.requireSectionInCourse(section.getId(), course.getId())).thenReturn(section);

		LessonResponse response = lessonService.create(
				course.getId(),
				section.getId(),
				new CreateLessonRequest(" Video 1 ", LessonType.VIDEO, " https://cdn/v ", 120, 3));

		ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
		verify(lessonRepository).save(captor.capture());
		Lesson saved = captor.getValue();
		assertThat(saved.getTitle()).isEqualTo("Video 1");
		assertThat(saved.getLessonType()).isEqualTo(LessonType.VIDEO);
		assertThat(saved.getContentUrl()).isEqualTo("https://cdn/v");
		assertThat(saved.getDurationSeconds()).isEqualTo(120);
		assertThat(saved.getSortOrder()).isEqualTo(3);
		assertThat(response.courseId()).isEqualTo(course.getId());
	}

	@Test
	void getById_directLookup_hidesUnpublishedFromNonAdmin() {
		Course course = course(false);
		Section section = section(course);
		Lesson lesson = lesson(section);
		when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));

		assertThatThrownBy(() -> lessonService.getById(lesson.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(enrollmentService, org.mockito.Mockito.never())
				.assertCanAccessLearningContent(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void getById_directLookup_allowsUnpublishedForAdmin_afterLearningAccessCheck() {
		asAdmin();
		Course course = course(false);
		Section section = section(course);
		Lesson lesson = lesson(section);
		when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));

		LessonResponse response = lessonService.getById(lesson.getId());

		verify(enrollmentService).assertCanAccessLearningContent(course.getId());
		assertThat(response.id()).isEqualTo(lesson.getId());
		assertThat(response.courseId()).isEqualTo(course.getId());
		assertThat(response.contentUrl()).isEqualTo("https://cdn/v");
	}

	@Test
	void getById_nested_rejectsWrongHierarchy() {
		UUID courseId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		UUID lessonId = UUID.randomUUID();
		when(sectionService.requireSectionInCourse(sectionId, courseId))
				.thenThrow(new ResourceNotFoundException("Section not found"));

		assertThatThrownBy(() -> lessonService.getById(courseId, sectionId, lessonId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_appendsSortOrderWhenOmitted() {
		Course course = course(true);
		Section section = section(course);
		Lesson existing = lesson(section);
		existing.setSortOrder(4);
		when(sectionService.requireSectionInCourse(section.getId(), course.getId())).thenReturn(section);
		when(lessonRepository.findBySectionIdOrderBySortOrderAsc(section.getId()))
				.thenReturn(List.of(existing));

		lessonService.create(
				course.getId(),
				section.getId(),
				new CreateLessonRequest(
						"Next",
						LessonType.ARTICLE,
						"https://example.com/article.html",
						null,
						null));

		ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
		verify(lessonRepository).save(captor.capture());
		assertThat(captor.getValue().getSortOrder()).isEqualTo(5);
	}

	@Test
	void reorder_appliesSequentialSortOrders() {
		Course course = course(true);
		Section section = section(course);
		Lesson first = lesson(section);
		first.setSortOrder(3);
		Lesson second = lesson(section);
		second.setId(UUID.randomUUID());
		second.setTitle("Second");
		second.setSortOrder(0);
		when(sectionService.requireSectionInCourse(section.getId(), course.getId())).thenReturn(section);
		when(lessonRepository.findBySectionIdOrderBySortOrderAsc(section.getId()))
				.thenReturn(List.of(first, second));

		lessonService.reorder(course.getId(), section.getId(), List.of(second.getId(), first.getId()));

		assertThat(second.getSortOrder()).isZero();
		assertThat(first.getSortOrder()).isEqualTo(1);
		verify(lessonRepository).saveAll(any());
	}

	@Test
	void update_changesMetadataAndOrder() {
		Course course = course(true);
		Section section = section(course);
		Lesson lesson = lesson(section);
		when(sectionService.requireSectionInCourse(section.getId(), course.getId())).thenReturn(section);
		when(lessonRepository.findByIdAndSectionId(lesson.getId(), section.getId()))
				.thenReturn(Optional.of(lesson));

		LessonResponse response = lessonService.update(
				course.getId(),
				section.getId(),
				lesson.getId(),
				new UpdateLessonRequest(
						"Article",
						LessonType.ARTICLE,
						"https://example.com/read.html",
						0,
						9));

		assertThat(response.title()).isEqualTo("Article");
		assertThat(response.lessonType()).isEqualTo(LessonType.ARTICLE);
		assertThat(response.sortOrder()).isEqualTo(9);
	}

	private static void asAdmin() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						"admin",
						"n/a",
						java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
	}

	private static Course course(boolean published) {
		User instructor = new User();
		instructor.setId(UUID.randomUUID());
		instructor.setRole(UserRole.INSTRUCTOR);
		instructor.setActive(true);

		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle("Course");
		course.setInstructor(instructor);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(published);
		return course;
	}

	private static Section section(Course course) {
		Section section = new Section();
		section.setId(UUID.randomUUID());
		section.setTitle("Section");
		section.setCourse(course);
		section.setSortOrder(0);
		return section;
	}

	private static Lesson lesson(Section section) {
		Lesson lesson = new Lesson();
		lesson.setId(UUID.randomUUID());
		lesson.setTitle("Lesson");
		lesson.setSection(section);
		lesson.setLessonType(LessonType.VIDEO);
		lesson.setContentUrl("https://cdn/v");
		lesson.setSortOrder(0);
		return lesson;
	}
}
