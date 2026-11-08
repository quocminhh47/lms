package com.lms.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReorderSupportTest {

	@Test
	void validateFullReorder_acceptsPermutation() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		assertThatCode(() -> ReorderSupport.validateFullReorder(List.of(a, b), List.of(b, a)))
				.doesNotThrowAnyException();
	}

	@Test
	void validateFullReorder_rejectsMissingId() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		assertThatThrownBy(() -> ReorderSupport.validateFullReorder(List.of(a, b), List.of(a)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void validateFullReorder_rejectsUnknownId() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		UUID extra = UUID.randomUUID();
		assertThatThrownBy(() -> ReorderSupport.validateFullReorder(List.of(a), List.of(extra)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
