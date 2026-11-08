package com.lms.repository.spec;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.lms.entity.Course;

import jakarta.persistence.criteria.JoinType;

public final class CourseSpecifications {

	private CourseSpecifications() {
	}

	public static Specification<Course> withPublished(Boolean published) {
		if (published == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("published"), published);
	}

	public static Specification<Course> withInstructorId(UUID instructorId) {
		if (instructorId == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("instructor").get("id"), instructorId);
	}

	public static Specification<Course> withSearch(String q) {
		if (q == null || q.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		String needle = "%" + q.trim().toLowerCase() + "%";
		return (root, query, cb) -> {
			var instructor = root.join("instructor", JoinType.LEFT);
			return cb.or(
					cb.like(cb.lower(root.get("title")), needle),
					cb.like(cb.lower(instructor.get("fullName")), needle),
					cb.like(cb.lower(instructor.get("email")), needle));
		};
	}

	public static Specification<Course> fetchInstructor() {
		return (root, query, cb) -> {
			if (query != null && !Long.class.equals(query.getResultType())) {
				root.fetch("instructor", JoinType.LEFT);
				query.distinct(true);
			}
			return null;
		};
	}
}
