package com.lms.mapper;

import org.springframework.stereotype.Component;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.entity.Enrollment;

@Component
public class EnrollmentMapper {

	public EnrollmentResponse toResponse(Enrollment enrollment) {
		return new EnrollmentResponse(
				enrollment.getId(),
				enrollment.getUser().getId(),
				enrollment.getCourse().getId(),
				enrollment.getCourse().getTitle(),
				enrollment.getStatus(),
				enrollment.getEnrolledAt());
	}
}
