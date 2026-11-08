package com.lms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.admin.CourseMetricsResponse;
import com.lms.dto.admin.StudentMetricsResponse;
import com.lms.dto.admin.UserMetricsResponse;
import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class AdminMetricsService {

	private final CourseRepository courseRepository;
	private final UserRepository userRepository;

	public AdminMetricsService(CourseRepository courseRepository, UserRepository userRepository) {
		this.courseRepository = courseRepository;
		this.userRepository = userRepository;
	}

	public CourseMetricsResponse courseMetrics() {
		long total = courseRepository.count();
		long published = courseRepository.countByPublished(true);
		long draft = courseRepository.countByPublished(false);
		long withLessons = courseRepository.countDistinctCoursesWithLessons();
		long emptyCurriculum = Math.max(0, total - withLessons);
		return new CourseMetricsResponse(total, published, draft, withLessons, emptyCurriculum);
	}

	public StudentMetricsResponse studentMetrics() {
		long activeStudents = userRepository.countByRoleAndActive(UserRole.STUDENT, true);
		return new StudentMetricsResponse(activeStudents);
	}

	public UserMetricsResponse userMetrics() {
		long total = userRepository.count();
		long active = userRepository.countByActive(true);
		long inactive = userRepository.countByActive(false);
		return new UserMetricsResponse(total, active, inactive);
	}
}
