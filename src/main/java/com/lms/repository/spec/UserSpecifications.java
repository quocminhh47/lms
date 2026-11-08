package com.lms.repository.spec;

import org.springframework.data.jpa.domain.Specification;

import com.lms.entity.User;
import com.lms.entity.UserRole;

public final class UserSpecifications {

	private UserSpecifications() {
	}

	public static Specification<User> withRole(UserRole role) {
		if (role == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("role"), role);
	}

	public static Specification<User> withActive(Boolean active) {
		if (active == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("active"), active);
	}

	public static Specification<User> withSearch(String q) {
		if (q == null || q.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		String needle = "%" + q.trim().toLowerCase() + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("fullName")), needle),
				cb.like(cb.lower(root.get("email")), needle),
				cb.like(cb.lower(root.get("phone")), needle));
	}
}
