package com.lms.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.lms.entity.Course;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Section;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.repository.CourseRepository;
import com.lms.repository.LessonRepository;
import com.lms.repository.SectionRepository;
import com.lms.repository.UserRepository;

@SpringBootTest
class CourseCatalogFieldsIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void publishedCourseList_includesThumbnailInstructorNameAndLessonCount() throws Exception {
		User instructor = seedInstructor("Dr. Nguyen");
		Course course = seedCourse(instructor, "Vietnamese 101", true);
		course.setThumbnailUrl("https://cdn.example/courses/vi101.png");
		courseRepository.save(course);

		Section section = seedSection(course, "Basics", 1);
		seedLesson(section, "Greetings", 1);
		seedLesson(section, "Numbers", 2);

		String courseId = course.getId().toString();
		mockMvc.perform(get("/api/courses/" + courseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(courseId))
				.andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.example/courses/vi101.png"))
				.andExpect(jsonPath("$.instructorName").value("Dr. Nguyen"))
				.andExpect(jsonPath("$.instructorId").value(instructor.getId().toString()))
				.andExpect(jsonPath("$.lessonCount").value(2));
	}

	@Test
	void userProfileUpdate_acceptsPhoneAndDateOfBirth() throws Exception {
		User student = seedStudent();
		String body = """
				{"phone":"+84 901 234 567","dateOfBirth":"1995-08-20"}
				""";

		mockMvc.perform(put("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone").value("+84 901 234 567"))
				.andExpect(jsonPath("$.dateOfBirth").value("1995-08-20"))
				.andExpect(jsonPath("$.role").value("STUDENT"));
	}

	@Test
	void userProfileUpdate_rejectsFutureDateOfBirth() throws Exception {
		User student = seedStudent();
		String body = """
				{"dateOfBirth":"2099-01-01"}
				""";

		mockMvc.perform(put("/api/users/" + student.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
	}

	private User seedInstructor(String fullName) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("ins-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName(fullName);
		user.setRole(UserRole.INSTRUCTOR);
		user.setActive(true);
		return userRepository.save(user);
	}

	private User seedStudent() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("stu-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setFullName("Student");
		user.setRole(UserRole.STUDENT);
		user.setActive(true);
		user.setDateOfBirth(LocalDate.of(2000, 1, 1));
		return userRepository.save(user);
	}

	private Course seedCourse(User instructor, String title, boolean published) {
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle(title);
		course.setInstructor(instructor);
		course.setPrice(BigDecimal.ZERO);
		course.setPublished(published);
		return courseRepository.save(course);
	}

	private Section seedSection(Course course, String title, int sortOrder) {
		Section section = new Section();
		section.setId(UUID.randomUUID());
		section.setTitle(title);
		section.setCourse(course);
		section.setSortOrder(sortOrder);
		return sectionRepository.save(section);
	}

	private Lesson seedLesson(Section section, String title, int sortOrder) {
		Lesson lesson = new Lesson();
		lesson.setId(UUID.randomUUID());
		lesson.setTitle(title);
		lesson.setSection(section);
		lesson.setLessonType(LessonType.ARTICLE);
		lesson.setSortOrder(sortOrder);
		return lessonRepository.save(lesson);
	}
}
