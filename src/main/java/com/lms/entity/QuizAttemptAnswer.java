package com.lms.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "quiz_attempt_answers",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_quiz_attempt_answers_attempt_question",
				columnNames = { "attempt_id", "question_id" }))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttemptAnswer {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id", nullable = false)
	private QuizAttempt attempt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(name = "text_answer", columnDefinition = "TEXT")
	private String textAnswer;

	@Column(name = "is_correct")
	private Boolean correct;
}
