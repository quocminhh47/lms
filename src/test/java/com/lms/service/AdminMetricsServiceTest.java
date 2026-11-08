package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminMetricsServiceTest {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AdminMetricsService adminMetricsService;

	@Test
	void courseMetrics_aggregatesCounts() {
		when(courseRepository.count()).thenReturn(10L);
		when(courseRepository.countByPublished(true)).thenReturn(6L);
		when(courseRepository.countByPublished(false)).thenReturn(4L);
		when(courseRepository.countDistinctCoursesWithLessons()).thenReturn(7L);

		var metrics = adminMetricsService.courseMetrics();

		assertThat(metrics.total()).isEqualTo(10);
		assertThat(metrics.published()).isEqualTo(6);
		assertThat(metrics.draft()).isEqualTo(4);
		assertThat(metrics.withLessons()).isEqualTo(7);
		assertThat(metrics.emptyCurriculum()).isEqualTo(3);
	}

	@Test
	void studentMetrics_countsActiveStudents() {
		when(userRepository.countByRoleAndActive(UserRole.STUDENT, true)).thenReturn(42L);

		assertThat(adminMetricsService.studentMetrics().activeStudentAccounts()).isEqualTo(42);
	}
}
