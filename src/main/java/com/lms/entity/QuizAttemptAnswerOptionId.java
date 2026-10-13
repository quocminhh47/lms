package com.lms.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class QuizAttemptAnswerOptionId implements Serializable {

	@Column(name = "attempt_answer_id", nullable = false)
	private UUID attemptAnswerId;

	@Column(name = "option_id", nullable = false)
	private UUID optionId;

	public QuizAttemptAnswerOptionId(UUID attemptAnswerId, UUID optionId) {
		this.attemptAnswerId = attemptAnswerId;
		this.optionId = optionId;
	}
}
