package com.lms.util;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class ReorderSupport {

	private ReorderSupport() {
	}

	public static void validateFullReorder(List<UUID> existingIds, List<UUID> orderedIds) {
		if (orderedIds == null || orderedIds.isEmpty()) {
			throw new IllegalArgumentException("orderedIds must not be empty");
		}
		if (orderedIds.size() != existingIds.size()) {
			throw new IllegalArgumentException("orderedIds must include every item");
		}
		if (!new HashSet<>(existingIds).equals(new HashSet<>(orderedIds))) {
			throw new IllegalArgumentException("orderedIds must match existing items");
		}
	}
}
