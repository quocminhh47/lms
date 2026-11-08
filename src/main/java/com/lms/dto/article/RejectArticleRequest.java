package com.lms.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectArticleRequest(
		@NotBlank @Size(max = 2000) String reason) {
}
