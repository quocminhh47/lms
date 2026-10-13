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

import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.entity.Course;
import com.lms.entity.Section;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.SectionMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.SectionRepository;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

	@Mock
	private SectionRepository sectionRepository;

	@Mock
	private CourseRepository courseRepository;

	private SectionService sectionService;

	@BeforeEach
	void setUp() {
		sectionService = new SectionService(sectionRepository, courseRepository, new SectionMapper());
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void create_persistsSectionForCourse() {
		Course course = course(false);
		when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
		when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));

		SectionResponse response = sectionService.create(
				course.getId(),
				new CreateSectionRequest(" Intro ", 2));

		ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
		verify(sectionRepository).save(captor.capture());
		assertThat(captor.getValue().getTitle()).isEqualTo("Intro");
		assertThat(captor.getValue().getSortOrder()).isEqualTo(2);
		assertThat(response.courseId()).isEqualTo(course.getId());
	}

	@Test
	void listByCourse_hidesUnpublishedFromNonAdmin() {
		Course course = course(false);
		when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> sectionService.listByCourse(course.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void getById_rejectsSectionFromOtherCourse() {
		Course course = course(true);
		UUID otherCourseId = UUID.randomUUID();
		when(sectionRepository.findByIdAndCourseId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> sectionService.getById(otherCourseId, UUID.randomUUID()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_changesTitleAndSortOrder() {
		Course course = course(true);
		Section section = section(course, "Old", 0);
		when(sectionRepository.findByIdAndCourseId(section.getId(), course.getId()))
				.thenReturn(Optional.of(section));
		when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));

		SectionResponse response = sectionService.update(
				course.getId(),
				section.getId(),
				new UpdateSectionRequest("New", 5));

		assertThat(response.title()).isEqualTo("New");
		assertThat(response.sortOrder()).isEqualTo(5);
	}

	@Test
	void listByCourse_allowsUnpublishedForAdmin() {
		asAdmin();
		Course course = course(false);
		Section section = section(course, "Draft Section", 1);
		when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
		when(sectionRepository.findByCourseIdOrderBySortOrderAsc(course.getId()))
				.thenReturn(List.of(section));

		List<SectionResponse> responses = sectionService.listByCourse(course.getId());

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).title()).isEqualTo("Draft Section");
	}

	private static void asAdmin() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						"admin",
						"n/a",
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
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

	private static Section section(Course course, String title, int sortOrder) {
		Section section = new Section();
		section.setId(UUID.randomUUID());
		section.setTitle(title);
		section.setCourse(course);
		section.setSortOrder(sortOrder);
		return section;
	}
}
