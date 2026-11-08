package com.lms.dto.common;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record ReorderIdsRequest(@NotEmpty List<UUID> orderedIds) {
}
