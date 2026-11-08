package com.lms.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.quiz.QuizAdminResponse;
import com.lms.dto.quiz.UpsertQuizOptionRequest;
import com.lms.dto.quiz.UpsertQuizQuestionRequest;
import com.lms.dto.quiz.UpsertQuizRequest;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Question;
import com.lms.entity.QuestionOption;
import com.lms.entity.QuestionType;
import com.lms.entity.Quiz;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.QuizAdminMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.LessonRepository;
import com.lms.repository.QuestionOptionRepository;
import com.lms.repository.QuestionRepository;
import com.lms.repository.QuizAttemptRepository;
import com.lms.repository.QuizRepository;

@Service
@Transactional(readOnly = true)
public class QuizAdminService {

	static final String QUIZ_QUESTIONS_LOCKED =
			"Cannot change quiz questions because attempts already exist";

	private static final int TEXT_MAX_LENGTH = 10_000;

	private final LessonRepository lessonRepository;
	private final QuizRepository quizRepository;
	private final QuestionRepository questionRepository;
	private final QuestionOptionRepository questionOptionRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final CourseRepository courseRepository;
	private final QuizAdminMapper quizAdminMapper;

	public QuizAdminService(
			LessonRepository lessonRepository,
			QuizRepository quizRepository,
			QuestionRepository questionRepository,
			QuestionOptionRepository questionOptionRepository,
			QuizAttemptRepository quizAttemptRepository,
			CourseRepository courseRepository,
			QuizAdminMapper quizAdminMapper) {
		this.lessonRepository = lessonRepository;
		this.quizRepository = quizRepository;
		this.questionRepository = questionRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.courseRepository = courseRepository;
		this.quizAdminMapper = quizAdminMapper;
	}

	public QuizAdminResponse getAdminQuiz(UUID courseId, UUID lessonId) {
		Lesson lesson = requireQuizLesson(courseId, lessonId);
		Quiz quiz = quizRepository.findByLessonId(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Quiz not configured for lesson: " + lessonId));
		return buildResponse(lesson, quiz);
	}

	@Transactional
	public QuizAdminResponse upsertAdminQuiz(UUID courseId, UUID lessonId, UpsertQuizRequest request) {
		Lesson lesson = requireQuizLesson(courseId, lessonId);
		validateSettings(request);

		Quiz quiz = quizRepository.findByLessonId(lessonId).orElse(null);
		boolean hasAttempts = quiz != null && quizAttemptRepository.existsByQuizId(quiz.getId());

		if (hasAttempts && request.questions() != null) {
			throw new ConflictException(QUIZ_QUESTIONS_LOCKED);
		}

		if (quiz == null) {
			quiz = createQuiz(lesson, request);
		} else {
			applySettings(quiz, request);
			quiz = quizRepository.save(quiz);
		}

		if (!hasAttempts && request.questions() != null) {
			validateQuestions(request.questions());
			replaceQuestions(quiz, request.questions());
		}

		return buildResponse(lesson, quiz);
	}

	private Quiz createQuiz(Lesson lesson, UpsertQuizRequest request) {
		Quiz quiz = new Quiz();
		quiz.setId(UUID.randomUUID());
		quiz.setLesson(lesson);
		quiz.setPassingScore(request.passingScore());
		quiz.setTimeLimitMinutes(request.timeLimitMinutes());
		quiz.setMaxAttempts(request.maxAttempts());
		return quizRepository.save(quiz);
	}

	private void applySettings(Quiz quiz, UpsertQuizRequest request) {
		quiz.setPassingScore(request.passingScore());
		quiz.setTimeLimitMinutes(request.timeLimitMinutes());
		quiz.setMaxAttempts(request.maxAttempts());
	}

	private void replaceQuestions(Quiz quiz, List<UpsertQuizQuestionRequest> questions) {
		questionOptionRepository.deleteByQuizId(quiz.getId());
		questionRepository.deleteByQuizId(quiz.getId());
		questionRepository.flush();

		int questionOrder = 0;
		for (UpsertQuizQuestionRequest questionRequest : questions) {
			Question question = new Question();
			question.setId(UUID.randomUUID());
			question.setQuiz(quiz);
			question.setQuestionText(questionRequest.questionText().trim());
			question.setQuestionType(questionRequest.questionType());
			question.setScoreWeight(
					questionRequest.scoreWeight() != null ? questionRequest.scoreWeight() : 1);
			question.setSortOrder(questionOrder++);
			questionRepository.save(question);

			int optionOrder = 0;
			for (UpsertQuizOptionRequest optionRequest : questionRequest.options()) {
				QuestionOption option = new QuestionOption();
				option.setId(UUID.randomUUID());
				option.setQuestion(question);
				option.setOptionText(optionRequest.optionText().trim());
				option.setCorrect(optionRequest.isCorrect());
				option.setSortOrder(optionOrder++);
				questionOptionRepository.save(option);
			}
		}
	}

	private QuizAdminResponse buildResponse(Lesson lesson, Quiz quiz) {
		UUID courseId = lesson.getSection().getCourse().getId();
		boolean finalAssessment = courseRepository.findByFinalQuizId(quiz.getId()).isPresent();
		List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quiz.getId());
		List<UUID> questionIds = questions.stream().map(Question::getId).toList();
		List<QuestionOption> options = questionIds.isEmpty()
				? List.of()
				: questionOptionRepository.findByQuestionIdInOrderBySortOrderAscIdAsc(questionIds);
		Map<UUID, List<QuestionOption>> grouped = quizAdminMapper.groupOptionsByQuestionId(options);
		return quizAdminMapper.toResponse(quiz, courseId, finalAssessment, questions, grouped);
	}

	private Lesson requireQuizLesson(UUID courseId, UUID lessonId) {
		Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Lesson not found: " + lessonId + " in course " + courseId));
		if (lesson.getLessonType() != LessonType.QUIZ) {
			throw new ResourceNotFoundException("Lesson is not a quiz lesson: " + lessonId);
		}
		return lesson;
	}

	private void validateSettings(UpsertQuizRequest request) {
		if (request.passingScore() < 0 || request.passingScore() > 100) {
			throw new BusinessRuleException("passingScore must be between 0 and 100");
		}
		if (request.timeLimitMinutes() != null && request.timeLimitMinutes() < 1) {
			throw new BusinessRuleException("timeLimitMinutes must be at least 1");
		}
		if (request.maxAttempts() != null && request.maxAttempts() < 1) {
			throw new BusinessRuleException("maxAttempts must be at least 1");
		}
	}

	private void validateQuestions(List<UpsertQuizQuestionRequest> questions) {
		if (questions.isEmpty()) {
			throw new BusinessRuleException("At least one question is required");
		}
		for (UpsertQuizQuestionRequest question : questions) {
			validateQuestion(question);
		}
	}

	private void validateQuestion(UpsertQuizQuestionRequest question) {
		String text = question.questionText() == null ? "" : question.questionText().trim();
		if (text.isEmpty()) {
			throw new BusinessRuleException("questionText must not be blank");
		}
		if (text.length() > TEXT_MAX_LENGTH) {
			throw new BusinessRuleException("questionText is too long");
		}
		int weight = question.scoreWeight() != null ? question.scoreWeight() : 1;
		if (weight < 1) {
			throw new BusinessRuleException("scoreWeight must be at least 1");
		}

		List<UpsertQuizOptionRequest> options = question.options();
		if (options == null || options.size() < 2) {
			throw new BusinessRuleException("Each question must have at least two options");
		}
		for (UpsertQuizOptionRequest option : options) {
			String optionText = option.optionText() == null ? "" : option.optionText().trim();
			if (optionText.isEmpty()) {
				throw new BusinessRuleException("optionText must not be blank");
			}
			if (optionText.length() > TEXT_MAX_LENGTH) {
				throw new BusinessRuleException("optionText is too long");
			}
		}

		long correctCount = options.stream().filter(UpsertQuizOptionRequest::isCorrect).count();
		QuestionType type = question.questionType();
		if (type == QuestionType.SINGLE_CHOICE && correctCount != 1) {
			throw new BusinessRuleException("SINGLE_CHOICE requires exactly one correct option");
		}
		if (type == QuestionType.TRUE_FALSE) {
			if (options.size() != 2) {
				throw new BusinessRuleException("TRUE_FALSE requires exactly two options");
			}
			if (correctCount != 1) {
				throw new BusinessRuleException("TRUE_FALSE requires exactly one correct option");
			}
		}
		if (type == QuestionType.MULTIPLE_CHOICE && correctCount < 1) {
			throw new BusinessRuleException("MULTIPLE_CHOICE requires at least one correct option");
		}
	}
}
