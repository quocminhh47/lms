package com.lms.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_attempt_answer_options")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttemptAnswerOption {

	@EmbeddedId
	private QuizAttemptAnswerOptionId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("attemptAnswerId")
	@JoinColumn(name = "attempt_answer_id", nullable = false)
	private QuizAttemptAnswer attemptAnswer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("optionId")
	@JoinColumn(name = "option_id", nullable = false)
	private QuestionOption option;
}
