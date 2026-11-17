package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.ApiException;
import com.miniudemy.course.domain.Category;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CreateCourseRequest;
import com.miniudemy.course.mapper.CategoryMapperImpl;
import com.miniudemy.course.mapper.CourseMapperImpl;
import com.miniudemy.course.repository.CategoryRepository;
import com.miniudemy.course.repository.CourseRepository;

/** Kurs oluşturma (#22, FR-07). Veritabanı ve auth modülü taklit; mapper'lar gerçek (MapStruct'ın ürettiği). */
class CourseServiceTest {

    private static final long INSTRUCTOR_ID = 7L;

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private CourseService courseService;

    private Category yazilim;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, categoryRepository, userDirectory,
                new CourseMapperImpl(new CategoryMapperImpl()));

        yazilim = new Category("Yazılım");
        ReflectionTestUtils.setField(yazilim, "id", 1L);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(yazilim));
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());
        given(userDirectory.findSummaries(List.of(INSTRUCTOR_ID)))
                .willReturn(Map.of(INSTRUCTOR_ID, new UserSummary(INSTRUCTOR_ID, "Mehmet Kaya")));
        given(courseRepository.saveAndFlush(any(Course.class))).willAnswer(call -> {
            Course course = call.getArgument(0);
            ReflectionTestUtils.setField(course, "id", 15L);
            return course;
        });
    }

    @Test
    void create_withValidRequest_savesDraftCourseOwnedByInstructor_andReturnsResponse() {
        CreateCourseRequest request = new CreateCourseRequest("Sıfırdan Java", "Java'ya temelden giriş kursu.", 1L,
                CourseLevel.BEGINNER, List.of("Nesne yönelimli programlama", "Koleksiyonlar"));

        CourseResponse response = courseService.create(INSTRUCTOR_ID, request);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getInstructorId()).isEqualTo(INSTRUCTOR_ID);
        assertThat(saved.getValue().getStatus()).isEqualTo(CourseStatus.DRAFT);

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.title()).isEqualTo("Sıfırdan Java");
        assertThat(response.description()).isEqualTo("Java'ya temelden giriş kursu.");
        assertThat(response.category()).isEqualTo(new CategoryResponse(1L, "Yazılım"));
        assertThat(response.level()).isEqualTo(CourseLevel.BEGINNER);
        assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
        assertThat(response.instructor()).isEqualTo(new UserSummary(INSTRUCTOR_ID, "Mehmet Kaya"));
        assertThat(response.learningOutcomes()).containsExactly("Nesne yönelimli programlama", "Koleksiyonlar");
        assertThat(response.coverUrl()).isNull();
        assertThat(response.rejectionReason()).isNull();
        assertThat(response.unpublishReason()).isNull();
    }

    @Test
    void create_withoutLearningOutcomes_returnsEmptyList() {
        CreateCourseRequest request = new CreateCourseRequest("Sıfırdan Java", "Java'ya temelden giriş kursu.", 1L,
                CourseLevel.BEGINNER, null);

        assertThat(courseService.create(INSTRUCTOR_ID, request).learningOutcomes()).isEmpty();
    }

    @Test
    void create_withUnknownCategory_returns400OnCategoryField_andSavesNothing() {
        CreateCourseRequest request = new CreateCourseRequest("Sıfırdan Java", "Java'ya temelden giriş kursu.", 99L,
                CourseLevel.BEGINNER, List.of());

        assertThatThrownBy(() -> courseService.create(INSTRUCTOR_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(api.getErrors()).isEqualTo(Map.of("categoryId", CourseService.CATEGORY_NOT_FOUND));
                });
        verify(courseRepository, never()).saveAndFlush(any());
    }
}
