package com.lms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.quiz.QuizAttemptAnswerResultResponse;
import com.lms.dto.quiz.QuizAttemptResultResponse;
import com.lms.dto.quiz.QuizAttemptTakeResponse;
import com.lms.dto.quiz.QuizQuestionStudentResponse;
import com.lms.dto.quiz.SubmitQuizAnswerRequest;
import com.lms.dto.quiz.SubmitQuizAttemptRequest;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Question;
import com.lms.entity.QuestionOption;
import com.lms.entity.Quiz;
import com.lms.entity.QuizAttempt;
import com.lms.entity.QuizAttemptAnswer;
import com.lms.entity.QuizAttemptAnswerOption;
import com.lms.entity.QuizAttemptAnswerOptionId;
import com.lms.entity.QuizAttemptStatus;
import com.lms.entity.User;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.QuizAttemptMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.LessonRepository;
import com.lms.repository.QuestionOptionRepository;
import com.lms.repository.QuestionRepository;
import com.lms.repository.QuizAttemptAnswerOptionRepository;
import com.lms.repository.QuizAttemptAnswerRepository;
import com.lms.repository.QuizAttemptRepository;
import com.lms.repository.QuizRepository;

@Service
@Transactional(readOnly = true)
public class QuizAttemptService {

	private static final String MAX_ATTEMPTS_MESSAGE = "Maximum quiz attempts reached";
	private static final String ATTEMPT_EXPIRED_MESSAGE = "Quiz attempt has expired";

	private final LessonRepository lessonRepository;
	private final QuizRepository quizRepository;
	private final QuestionRepository questionRepository;
	private final QuestionOptionRepository questionOptionRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
	private final QuizAttemptAnswerOptionRepository quizAttemptAnswerOptionRepository;
	private final CourseRepository courseRepository;
	private final EnrollmentService enrollmentService;
	private final UserService userService;
	private final QuizAttemptMapper quizAttemptMapper;

	public QuizAttemptService(
			LessonRepository lessonRepository,
			QuizRepository quizRepository,
			QuestionRepository questionRepository,
			QuestionOptionRepository questionOptionRepository,
			QuizAttemptRepository quizAttemptRepository,
			QuizAttemptAnswerRepository quizAttemptAnswerRepository,
			QuizAttemptAnswerOptionRepository quizAttemptAnswerOptionRepository,
			CourseRepository courseRepository,
			EnrollmentService enrollmentService,
			UserService userService,
			QuizAttemptMapper quizAttemptMapper) {
		this.lessonRepository = lessonRepository;
		this.quizRepository = quizRepository;
		this.questionRepository = questionRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
		this.quizAttemptAnswerOptionRepository = quizAttemptAnswerOptionRepository;
		this.courseRepository = courseRepository;
		this.enrollmentService = enrollmentService;
		this.userService = userService;
		this.quizAttemptMapper = quizAttemptMapper;
	}

	public Object getSession(UUID courseId, UUID lessonId, UUID userId) {
		QuizContext context = requireQuizContext(courseId, lessonId);
		enrollmentService.assertCanAccessLearningContent(courseId);
		expireStaleInProgressAttempts(userId, context.quiz().getId(), OffsetDateTime.now());

		Optional<QuizAttempt> inProgress = quizAttemptRepository
				.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(
						userId, context.quiz().getId(), QuizAttemptStatus.IN_PROGRESS);
		if (inProgress.isPresent() && !isExpired(inProgress.get(), OffsetDateTime.now())) {
			return buildTakeResponse(inProgress.get(), context, userId);
		}

		Optional<QuizAttempt> latestSubmitted = quizAttemptRepository
				.findFirstByUserIdAndQuizIdAndStatusOrderBySubmittedAtDesc(
						userId, context.quiz().getId(), QuizAttemptStatus.SUBMITTED);
		if (latestSubmitted.isPresent()) {
			return buildResultResponse(latestSubmitted.get(), context, userId);
		}

		throw new ResourceNotFoundException("No quiz attempt session for lesson: " + lessonId);
	}

	@Transactional
	public QuizAttemptTakeResponse startOrResume(UUID courseId, UUID lessonId, UUID userId) {
		QuizContext context = requireQuizContext(courseId, lessonId);
		enrollmentService.assertCanAccessLearningContent(courseId);
		OffsetDateTime now = OffsetDateTime.now();
		expireStaleInProgressAttempts(userId, context.quiz().getId(), now);

		Optional<QuizAttempt> inProgress = quizAttemptRepository
				.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(
						userId, context.quiz().getId(), QuizAttemptStatus.IN_PROGRESS);
		if (inProgress.isPresent() && !isExpired(inProgress.get(), now)) {
			return buildTakeResponse(inProgress.get(), context, userId);
		}

		assertCanStartNewAttempt(userId, context.quiz(), now);
		QuizAttempt attempt = createAttempt(userId, context.quiz(), now);
		return buildTakeResponse(attempt, context, userId);
	}

	public QuizAttemptTakeResponse getCurrent(UUID courseId, UUID lessonId, UUID userId) {
		QuizContext context = requireQuizContext(courseId, lessonId);
		enrollmentService.assertCanAccessLearningContent(courseId);
		OffsetDateTime now = OffsetDateTime.now();
		expireStaleInProgressAttempts(userId, context.quiz().getId(), now);

		QuizAttempt attempt = quizAttemptRepository
				.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(
						userId, context.quiz().getId(), QuizAttemptStatus.IN_PROGRESS)
				.filter(candidate -> !isExpired(candidate, now))
				.orElseThrow(() -> new ResourceNotFoundException(
						"No in-progress quiz attempt for lesson: " + lessonId));
		return buildTakeResponse(attempt, context, userId);
	}

	public Object getAttempt(UUID courseId, UUID lessonId, UUID attemptId, UUID userId) {
		QuizContext context = requireQuizContext(courseId, lessonId);
		QuizAttempt attempt = requireOwnedAttempt(attemptId, userId, context.quiz().getId());
		enrollmentService.assertCanAccessLearningContent(courseId);
		if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS) {
			if (isExpired(attempt, OffsetDateTime.now())) {
				throw new ConflictException(ATTEMPT_EXPIRED_MESSAGE);
			}
			return buildTakeResponse(attempt, context, userId);
		}
		if (attempt.getStatus() == QuizAttemptStatus.SUBMITTED) {
			return buildResultResponse(attempt, context, userId);
		}
		throw new ResourceNotFoundException("Quiz attempt not available: " + attemptId);
	}

	@Transactional
	public QuizAttemptResultResponse submit(
			UUID courseId,
			UUID lessonId,
			UUID attemptId,
			UUID userId,
			SubmitQuizAttemptRequest request) {
		QuizContext context = requireQuizContext(courseId, lessonId);
		OffsetDateTime now = OffsetDateTime.now();

		QuizAttempt attempt = requireOwnedAttempt(attemptId, userId, context.quiz().getId());
		enrollmentService.assertCanAccessLearningContent(courseId);
		if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
			throw new ConflictException("Quiz attempt is not in progress");
		}
		if (isExpired(attempt, now)) {
			attempt.setStatus(QuizAttemptStatus.EXPIRED);
			quizAttemptRepository.save(attempt);
			throw new ConflictException(ATTEMPT_EXPIRED_MESSAGE);
		}

		List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(
				context.quiz().getId());
		if (questions.isEmpty()) {
			throw new BusinessRuleException("Quiz has no questions");
		}

		Map<UUID, Question> questionsById = questions.stream()
				.collect(Collectors.toMap(Question::getId, question -> question));
		Map<UUID, List<QuestionOption>> optionsByQuestionId = loadOptionsByQuestionId(questions);

		validateSubmitPayload(request, questionsById, optionsByQuestionId);

		int totalWeight = questions.stream().mapToInt(Question::getScoreWeight).sum();
		int earnedWeight = 0;

		for (SubmitQuizAnswerRequest answerRequest : request.answers()) {
			Question question = questionsById.get(answerRequest.questionId());
			Set<UUID> selected = new HashSet<>(answerRequest.selectedOptionIds());
			Set<UUID> correct = optionsByQuestionId.getOrDefault(question.getId(), List.of()).stream()
					.filter(QuestionOption::isCorrect)
					.map(QuestionOption::getId)
					.collect(Collectors.toSet());
			boolean answerCorrect = selected.equals(correct);
			if (answerCorrect) {
				earnedWeight += question.getScoreWeight();
			}

			QuizAttemptAnswer answer = new QuizAttemptAnswer();
			answer.setId(UUID.randomUUID());
			answer.setAttempt(attempt);
			answer.setQuestion(question);
			answer.setCorrect(answerCorrect);
			quizAttemptAnswerRepository.save(answer);

			for (UUID optionId : selected) {
				QuestionOption option = optionsByQuestionId.get(question.getId()).stream()
						.filter(candidate -> candidate.getId().equals(optionId))
						.findFirst()
						.orElseThrow();
				QuizAttemptAnswerOption link = new QuizAttemptAnswerOption();
				link.setId(new QuizAttemptAnswerOptionId(answer.getId(), optionId));
				link.setAttemptAnswer(answer);
				link.setOption(option);
				quizAttemptAnswerOptionRepository.save(link);
			}
		}

		BigDecimal score = totalWeight == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(earnedWeight)
						.multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
		boolean passed = score.compareTo(BigDecimal.valueOf(context.quiz().getPassingScore())) >= 0;

		attempt.setScore(score);
		attempt.setPassed(passed);
		attempt.setSubmittedAt(now);
		attempt.setStatus(QuizAttemptStatus.SUBMITTED);
		quizAttemptRepository.save(attempt);

		return buildResultResponse(attempt, context, userId);
	}

	private QuizAttempt createAttempt(UUID userId, Quiz quiz, OffsetDateTime now) {
		User user = userService.requireUser(userId);
		QuizAttempt attempt = new QuizAttempt();
		attempt.setId(UUID.randomUUID());
		attempt.setUser(user);
		attempt.setQuiz(quiz);
		attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
		if (quiz.getTimeLimitMinutes() != null) {
			attempt.setExpiresAt(now.plusMinutes(quiz.getTimeLimitMinutes()));
		}
		return quizAttemptRepository.saveAndFlush(attempt);
	}

	private void assertCanStartNewAttempt(UUID userId, Quiz quiz, OffsetDateTime now) {
		expireStaleInProgressAttempts(userId, quiz.getId(), now);
		Integer maxAttempts = quiz.getMaxAttempts();
		if (maxAttempts == null) {
			return;
		}
		long used = countAttemptsUsed(userId, quiz.getId());
		if (used >= maxAttempts) {
			throw new ConflictException(MAX_ATTEMPTS_MESSAGE);
		}
	}

	private void expireStaleInProgressAttempts(UUID userId, UUID quizId, OffsetDateTime now) {
		List<QuizAttempt> inProgress = quizAttemptRepository.findByUserIdAndQuizIdAndStatus(
				userId, quizId, QuizAttemptStatus.IN_PROGRESS);
		for (QuizAttempt attempt : inProgress) {
			if (isExpired(attempt, now)) {
				attempt.setStatus(QuizAttemptStatus.EXPIRED);
				quizAttemptRepository.save(attempt);
			}
		}
	}

	private boolean isExpired(QuizAttempt attempt, OffsetDateTime now) {
		return attempt.getExpiresAt() != null && now.isAfter(attempt.getExpiresAt());
	}

	private long countAttemptsUsed(UUID userId, UUID quizId) {
		return quizAttemptRepository.countByUserIdAndQuizIdAndStatusNot(
				userId, quizId, QuizAttemptStatus.EXPIRED);
	}

	private QuizAttempt requireOwnedAttempt(UUID attemptId, UUID userId, UUID quizId) {
		QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found: " + attemptId));
		if (!attempt.getQuiz().getId().equals(quizId)) {
			throw new ResourceNotFoundException("Quiz attempt not found: " + attemptId);
		}
		return attempt;
	}

	private void validateSubmitPayload(
			SubmitQuizAttemptRequest request,
			Map<UUID, Question> questionsById,
			Map<UUID, List<QuestionOption>> optionsByQuestionId) {
		Set<UUID> seenQuestions = new HashSet<>();
		for (SubmitQuizAnswerRequest answer : request.answers()) {
			if (!seenQuestions.add(answer.questionId())) {
				throw new BusinessRuleException("Duplicate answer for question: " + answer.questionId());
			}
			Question question = questionsById.get(answer.questionId());
			if (question == null) {
				throw new BusinessRuleException("Unknown question: " + answer.questionId());
			}
			Set<UUID> optionIds = optionsByQuestionId.getOrDefault(question.getId(), List.of()).stream()
					.map(QuestionOption::getId)
					.collect(Collectors.toSet());
			for (UUID selectedId : answer.selectedOptionIds()) {
				if (!optionIds.contains(selectedId)) {
					throw new BusinessRuleException(
							"Option does not belong to question: " + selectedId);
				}
			}
			if (answer.selectedOptionIds().size() != new HashSet<>(answer.selectedOptionIds()).size()) {
				throw new BusinessRuleException("Duplicate selected options for question: " + question.getId());
			}
		}
		if (seenQuestions.size() != questionsById.size()) {
			throw new BusinessRuleException("Answers must be provided for every question");
		}
	}

	private Map<UUID, List<QuestionOption>> loadOptionsByQuestionId(List<Question> questions) {
		List<UUID> questionIds = questions.stream().map(Question::getId).toList();
		if (questionIds.isEmpty()) {
			return Map.of();
		}
		List<QuestionOption> options = questionOptionRepository
				.findByQuestionIdInOrderBySortOrderAscIdAsc(questionIds);
		return options.stream().collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
	}

	private QuizAttemptTakeResponse buildTakeResponse(QuizAttempt attempt, QuizContext context, UUID userId) {
		List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(
				context.quiz().getId());
		Map<UUID, List<QuestionOption>> optionsByQuestionId = loadOptionsByQuestionId(questions);
		List<QuizQuestionStudentResponse> studentQuestions = quizAttemptMapper.toStudentQuestions(
				questions, optionsByQuestionId);
		int attemptsUsed = (int) countAttemptsUsed(userId, context.quiz().getId());
		return quizAttemptMapper.toTakeResponse(
				attempt,
				context.quiz(),
				context.lesson().getId(),
				context.finalAssessment(),
				attemptsUsed,
				studentQuestions);
	}

	private QuizAttemptResultResponse buildResultResponse(QuizAttempt attempt, QuizContext context, UUID userId) {
		List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(
				context.quiz().getId());
		Map<UUID, List<QuestionOption>> optionsByQuestionId = loadOptionsByQuestionId(questions);
		List<QuizQuestionStudentResponse> studentQuestions = quizAttemptMapper.toStudentQuestions(
				questions, optionsByQuestionId);

		List<QuizAttemptAnswer> answers = quizAttemptAnswerRepository.findByAttemptId(attempt.getId());
		List<QuizAttemptAnswerOption> answerOptions = answers.isEmpty()
				? List.of()
				: loadAnswerOptions(answers);
		List<QuizAttemptAnswerResultResponse> answerResults = quizAttemptMapper.toAnswerResults(
				answers, answerOptions);

		BestResult best = computeBestSubmitted(userId, context.quiz().getId());
		int attemptsUsed = (int) countAttemptsUsed(userId, context.quiz().getId());

		return quizAttemptMapper.toResultResponse(
				attempt,
				context.quiz(),
				context.lesson().getId(),
				context.finalAssessment(),
				attemptsUsed,
				best.score(),
				best.passed(),
				answerResults,
				studentQuestions);
	}

	private List<QuizAttemptAnswerOption> loadAnswerOptions(List<QuizAttemptAnswer> answers) {
		List<QuizAttemptAnswerOption> result = new ArrayList<>();
		for (QuizAttemptAnswer answer : answers) {
			result.addAll(quizAttemptAnswerOptionRepository.findByAttemptAnswerId(answer.getId()));
		}
		return result;
	}

	private BestResult computeBestSubmitted(UUID userId, UUID quizId) {
		List<QuizAttempt> submitted = quizAttemptRepository.findByUserIdAndQuizIdAndStatus(
				userId, quizId, QuizAttemptStatus.SUBMITTED);
		Optional<QuizAttempt> best = submitted.stream()
				.filter(attempt -> attempt.getScore() != null)
				.max(Comparator.comparing(QuizAttempt::getScore));
		if (best.isEmpty()) {
			return new BestResult(null, null);
		}
		return new BestResult(best.get().getScore(), best.get().getPassed());
	}

	private QuizContext requireQuizContext(UUID courseId, UUID lessonId) {
		Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Lesson not found: " + lessonId + " in course " + courseId));
		if (lesson.getLessonType() != LessonType.QUIZ) {
			throw new ResourceNotFoundException("Lesson is not a quiz lesson: " + lessonId);
		}
		Quiz quiz = quizRepository.findByLessonId(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Quiz not configured for lesson: " + lessonId));
		boolean finalAssessment = courseRepository.findByFinalQuizId(quiz.getId()).isPresent();
		return new QuizContext(lesson, quiz, courseId, finalAssessment);
	}

	private record QuizContext(Lesson lesson, Quiz quiz, UUID courseId, boolean finalAssessment) {
	}

	private record BestResult(BigDecimal score, Boolean passed) {
	}
}
