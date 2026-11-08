package com.lms.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.lms.exception.BusinessRuleException;

public final class PagingSupport {

	public static final int DEFAULT_PAGE = 0;
	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	private PagingSupport() {
	}

	public static Pageable pageable(int page, int size, Sort sort) {
		if (page < 0) {
			throw new BusinessRuleException("page must be >= 0");
		}
		int effectiveSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		if (sort == null || sort.isUnsorted()) {
			return PageRequest.of(page, effectiveSize);
		}
		return PageRequest.of(page, effectiveSize, sort);
	}

	public static Sort parseSort(String sortParam, Sort defaultSort, String... allowedProperties) {
		if (sortParam == null || sortParam.isBlank()) {
			return defaultSort;
		}
		String[] parts = sortParam.split(",", 2);
		String property = parts[0].trim();
		boolean allowed = false;
		for (String candidate : allowedProperties) {
			if (candidate.equals(property)) {
				allowed = true;
				break;
			}
		}
		if (!allowed) {
			throw new BusinessRuleException("Unsupported sort field: " + property);
		}
		Sort.Direction direction = Sort.Direction.DESC;
		if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
			direction = Sort.Direction.ASC;
		}
		return Sort.by(direction, property);
	}
}
