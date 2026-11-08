package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.dto.quiz.UpsertQuizOptionRequest;
import com.lms.dto.quiz.UpsertQuizQuestionRequest;
import com.lms.dto.quiz.UpsertQuizRequest;
import com.lms.entity.Course;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.QuestionType;
import com.lms.entity.Quiz;
import com.lms.entity.Section;
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

@ExtendWith(MockitoExtension.class)
class QuizAdminServiceTest {

	@Mock
	private LessonRepository lessonRepository;
	@Mock
	private QuizRepository quizRepository;
	@Mock
	private QuestionRepository questionRepository;
	@Mock
	private QuestionOptionRepository questionOptionRepository;
	@Mock
	private QuizAttemptRepository quizAttemptRepository;
	@Mock
	private CourseRepository courseRepository;

	private QuizAdminService quizAdminService;

	private final UUID courseId = UUID.randomUUID();
	private final UUID lessonId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		quizAdminService = new QuizAdminService(
				lessonRepository,
				quizRepository,
				questionRepository,
				questionOptionRepository,
				quizAttemptRepository,
				courseRepository,
				new QuizAdminMapper());
	}

	@Test
	void getAdminQuiz_requiresExistingQuiz() {
		when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(quizLesson()));
		when(quizRepository.findByLessonId(lessonId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> quizAdminService.getAdminQuiz(courseId, lessonId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void upsert_rejectsQuestionChangesWhenAttemptsExist() {
		Quiz quiz = quizEntity();
		when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(quizLesson()));
		when(quizRepository.findByLessonId(lessonId)).thenReturn(Optional.of(quiz));
		when(quizAttemptRepository.existsByQuizId(quiz.getId())).thenReturn(true);

		UpsertQuizRequest request = new UpsertQuizRequest(
				80,
				null,
				null,
				List.of(validQuestion()));

		assertThatThrownBy(() -> quizAdminService.upsertAdminQuiz(courseId, lessonId, request))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("attempts");
	}

	@Test
	void upsert_allowsSettingsOnlyWhenAttemptsExist() {
		Quiz quiz = quizEntity();
		when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(quizLesson()));
		when(quizRepository.findByLessonId(lessonId)).thenReturn(Optional.of(quiz));
		when(quizAttemptRepository.existsByQuizId(quiz.getId())).thenReturn(true);
		when(quizRepository.save(quiz)).thenReturn(quiz);
		when(courseRepository.findByFinalQuizId(quiz.getId())).thenReturn(Optional.empty());
		when(questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quiz.getId())).thenReturn(List.of());

		quizAdminService.upsertAdminQuiz(courseId, lessonId, new UpsertQuizRequest(90, 30, 2, null));

		verify(questionOptionRepository, never()).deleteByQuizId(any());
		verify(questionRepository, never()).deleteByQuizId(any());
		assertThat(quiz.getPassingScore()).isEqualTo(90);
		assertThat(quiz.getTimeLimitMinutes()).isEqualTo(30);
		assertThat(quiz.getMaxAttempts()).isEqualTo(2);
	}

	@Test
	void upsert_rejectsSingleChoiceWithoutExactlyOneCorrect() {
		when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(quizLesson()));
		when(quizRepository.findByLessonId(lessonId)).thenReturn(Optional.empty());
		when(quizRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UpsertQuizQuestionRequest question = new UpsertQuizQuestionRequest(
				"Pick one",
				QuestionType.SINGLE_CHOICE,
				1,
				List.of(
						new UpsertQuizOptionRequest("A", false),
						new UpsertQuizOptionRequest("B", false)));

		assertThatThrownBy(() -> quizAdminService.upsertAdminQuiz(
						courseId, lessonId, new UpsertQuizRequest(80, null, null, List.of(question))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void upsert_rejectsLessonCourseMismatch() {
		when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> quizAdminService.upsertAdminQuiz(
						courseId, lessonId, new UpsertQuizRequest(80, null, null, null)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private Lesson quizLesson() {
		Course course = new Course();
		course.setId(courseId);
		Section section = new Section();
		section.setCourse(course);
		Lesson lesson = new Lesson();
		lesson.setId(lessonId);
		lesson.setSection(section);
		lesson.setLessonType(LessonType.QUIZ);
		lesson.setTitle("Quiz lesson");
		return lesson;
	}

	private Quiz quizEntity() {
		Quiz quiz = new Quiz();
		quiz.setId(UUID.randomUUID());
		quiz.setLesson(quizLesson());
		quiz.setPassingScore(80);
		return quiz;
	}

	private UpsertQuizQuestionRequest validQuestion() {
		return new UpsertQuizQuestionRequest(
				"Question?",
				QuestionType.SINGLE_CHOICE,
				1,
				List.of(
						new UpsertQuizOptionRequest("Yes", true),
						new UpsertQuizOptionRequest("No", false)));
	}
}
