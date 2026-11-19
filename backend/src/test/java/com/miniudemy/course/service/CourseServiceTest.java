package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.ApiException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Category;
import com.miniudemy.course.domain.ContentFileDetached;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CreateCourseRequest;
import com.miniudemy.course.dto.UpdateCourseRequest;
import com.miniudemy.course.mapper.CategoryMapperImpl;
import com.miniudemy.course.mapper.CourseMapperImpl;
import com.miniudemy.course.repository.CategoryRepository;
import com.miniudemy.course.repository.SectionRepository;
import com.miniudemy.course.repository.CourseRepository;

/** Kurs oluşturma (#22, FR-07). Veritabanı ve auth modülü taklit; mapper'lar gerçek (MapStruct'ın ürettiği). */
class CourseServiceTest {

    private static final long INSTRUCTOR_ID = 7L;

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final EnrollmentCounts enrollmentCounts = mock(EnrollmentCounts.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private CourseService courseService;

    private Category yazilim;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, categoryRepository, userDirectory,
                new CourseMapperImpl(new CategoryMapperImpl()), new CourseOwnershipService(courseRepository,
                        mock(SectionRepository.class)), enrollmentCounts, events);

        yazilim = new Category("Yazılım");
        ReflectionTestUtils.setField(yazilim, "id", 1L);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(yazilim));
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());
        given(userDirectory.findSummaries(List.of(INSTRUCTOR_ID)))
                .willReturn(Map.of(INSTRUCTOR_ID, new UserSummary(INSTRUCTOR_ID, "Mehmet Kaya")));
        given(courseRepository.saveAndFlush(any(Course.class))).willAnswer(call -> {
            Course course = call.getArgument(0);
            if (course.getId() == null) {
                ReflectionTestUtils.setField(course, "id", 15L); // yeni kurs; var olan kursun id'si değişmez
            }
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

    @Test
    void listByInstructor_returnsOwnCoursesInRepositoryOrder_withInstructorAndReasons() {
        Course rejected = CourseFixtures.course(21L, INSTRUCTOR_ID, "Reddedilen", CourseStatus.DRAFT);
        ReflectionTestUtils.setField(rejected, "rejectionReason", "Örnekler yetersiz.");
        Course unpublished = CourseFixtures.course(20L, INSTRUCTOR_ID, "Kaldırılan", CourseStatus.UNPUBLISHED);
        ReflectionTestUtils.setField(unpublished, "unpublishReason", "Güncel değil.");
        given(courseRepository.findByInstructor(INSTRUCTOR_ID, null)).willReturn(List.of(rejected, unpublished));

        List<CourseResponse> result = courseService.listByInstructor(INSTRUCTOR_ID, null);

        assertThat(result).extracting(CourseResponse::id).containsExactly(21L, 20L);
        assertThat(result.get(0).rejectionReason()).isEqualTo("Örnekler yetersiz.");
        assertThat(result.get(1).unpublishReason()).isEqualTo("Güncel değil.");
        assertThat(result).allSatisfy(r -> {
            assertThat(r.instructor()).isEqualTo(new UserSummary(INSTRUCTOR_ID, "Mehmet Kaya"));
            assertThat(r.category()).isEqualTo(new CategoryResponse(1L, "Yazılım"));
        });
        // Eğitmen adı kurs sayısından bağımsız tek çağrıyla alınır (N+1 yok).
        verify(userDirectory, times(1)).findSummaries(any());
    }

    @Test
    void listByInstructor_queriesOnlyTheGivenInstructor_soOthersCoursesNeverAppear() {
        courseService.listByInstructor(INSTRUCTOR_ID, null);

        verify(courseRepository).findByInstructor(INSTRUCTOR_ID, null);
        verify(courseRepository, never()).findAll();
    }

    @Test
    void listByInstructor_withStatus_passesFilterToRepository() {
        Course draft = CourseFixtures.course(22L, INSTRUCTOR_ID, "Taslak", CourseStatus.DRAFT);
        given(courseRepository.findByInstructor(INSTRUCTOR_ID, CourseStatus.DRAFT)).willReturn(List.of(draft));

        List<CourseResponse> result = courseService.listByInstructor(INSTRUCTOR_ID, CourseStatus.DRAFT);

        assertThat(result).extracting(CourseResponse::status).containsExactly(CourseStatus.DRAFT);
    }

    @Test
    void listByInstructor_withNoCourses_returnsEmptyList_withoutLookingUpUsers() {
        given(courseRepository.findByInstructor(INSTRUCTOR_ID, null)).willReturn(List.of());

        assertThat(courseService.listByInstructor(INSTRUCTOR_ID, null)).isEmpty();
        verify(userDirectory, never()).findSummaries(any());
    }

    // --- düzenleme (#23) ---

    private static final long OTHER_INSTRUCTOR_ID = 8L;

    private Course stored(long id, CourseStatus status) {
        Course course = CourseFixtures.course(id, INSTRUCTOR_ID, "Eski başlık", status);
        given(courseRepository.findById(id)).willReturn(Optional.of(course));
        return course;
    }

    private static UpdateCourseRequest updateRequest(long categoryId) {
        return new UpdateCourseRequest("Yeni başlık", "Yeni ve daha uzun açıklama.", categoryId,
                CourseLevel.ADVANCED, List.of("Yeni hedef"));
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"DRAFT", "UNPUBLISHED"})
    void update_ownCourseInEditableState_changesAllFields_andReturnsResponse(CourseStatus status) {
        Category tasarim = new Category("Tasarım");
        ReflectionTestUtils.setField(tasarim, "id", 2L);
        given(categoryRepository.findById(2L)).willReturn(Optional.of(tasarim));
        Course course = stored(30L, status);

        CourseResponse response = courseService.update(30L, INSTRUCTOR_ID, updateRequest(2L));

        assertThat(course.getTitle()).isEqualTo("Yeni başlık");
        assertThat(course.getDescription()).isEqualTo("Yeni ve daha uzun açıklama.");
        assertThat(course.getCategory()).isSameAs(tasarim);
        assertThat(course.getLevel()).isEqualTo(CourseLevel.ADVANCED);
        assertThat(course.getLearningOutcomes()).containsExactly("Yeni hedef");
        assertThat(course.getStatus()).isEqualTo(status);
        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.title()).isEqualTo("Yeni başlık");
        assertThat(response.category()).isEqualTo(new CategoryResponse(2L, "Tasarım"));
        assertThat(response.instructor()).isEqualTo(new UserSummary(INSTRUCTOR_ID, "Mehmet Kaya"));
    }

    @Test
    void update_withoutLearningOutcomes_storesAnEmptyList() {
        Course course = stored(30L, CourseStatus.DRAFT);

        courseService.update(30L, INSTRUCTOR_ID, new UpdateCourseRequest("Yeni başlık", "Yeni ve daha uzun açıklama.",
                1L, CourseLevel.BEGINNER, null));

        assertThat(course.getLearningOutcomes()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"IN_REVIEW", "PUBLISHED"})
    void update_inReviewOrPublished_returns409_andChangesNothing(CourseStatus status) {
        Course course = stored(30L, status);

        assertThatThrownBy(() -> courseService.update(30L, INSTRUCTOR_ID, updateRequest(1L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(course.getTitle()).isEqualTo("Eski başlık");
    }

    @Test
    void update_otherInstructorsCourse_returns403_andChangesNothing() {
        Course course = stored(30L, CourseStatus.DRAFT);

        assertThatThrownBy(() -> courseService.update(30L, OTHER_INSTRUCTOR_ID, updateRequest(1L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(course.getTitle()).isEqualTo("Eski başlık");
    }

    @Test
    void update_missingCourse_returns404() {
        given(courseRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(404L, INSTRUCTOR_ID, updateRequest(1L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void update_unknownCategory_returns400WithCategoryFieldError_andChangesNothing() {
        Course course = stored(30L, CourseStatus.DRAFT);

        assertThatThrownBy(() -> courseService.update(30L, INSTRUCTOR_ID, updateRequest(99L)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrors()).containsKey("categoryId");
                });
        assertThat(course.getTitle()).isEqualTo("Eski başlık");
    }

    // --- silme (#23) ---

    private Course draftWithFiles() {
        Course course = stored(40L, CourseStatus.DRAFT);
        ReflectionTestUtils.setField(course, "coverFileId", 300L);
        var section = CourseFixtures.section(course, 1L, "Giriş", 0);
        CourseFixtures.lesson(section, 11L, "Video", 60, 0, 301L);
        CourseFixtures.lesson(section, 12L, "İçeriksiz", 60, 1, null);
        var second = CourseFixtures.section(course, 2L, "Devam", 1);
        CourseFixtures.lesson(second, 21L, "PDF", 0, 0, 302L);
        return course;
    }

    @Test
    void delete_ownDraftCourseWithoutStudents_deletesIt_andDetachesCoverAndLessonFiles() {
        Course course = draftWithFiles();
        given(enrollmentCounts.countByCourse(40L)).willReturn(0L);

        courseService.delete(40L, INSTRUCTOR_ID);

        verify(courseRepository).delete(course);
        verify(events).publishEvent(new ContentFileDetached(300L));
        verify(events).publishEvent(new ContentFileDetached(301L));
        verify(events).publishEvent(new ContentFileDetached(302L));
        verify(events, times(3)).publishEvent(any(Object.class));
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"IN_REVIEW", "PUBLISHED", "UNPUBLISHED"})
    void delete_notDraft_returns409_andDeletesNothing(CourseStatus status) {
        stored(40L, status);

        assertThatThrownBy(() -> courseService.delete(40L, INSTRUCTOR_ID))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(courseRepository, never()).delete(any(Course.class));
        verifyNoInteractions(events);
    }

    @Test
    void delete_draftCourseThatHasStudents_returns409_andDeletesNothing() {
        // Yayımlanıp kursiyer almış, sonra taslağa dönmüş kurs (sözleşme › deleteCourse).
        draftWithFiles();
        given(enrollmentCounts.countByCourse(40L)).willReturn(2L);

        assertThatThrownBy(() -> courseService.delete(40L, INSTRUCTOR_ID))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("kursiyer");
                });
        verify(courseRepository, never()).delete(any(Course.class));
        verifyNoInteractions(events);
    }

    @Test
    void delete_otherInstructorsCourse_returns403_andDeletesNothing() {
        draftWithFiles();

        assertThatThrownBy(() -> courseService.delete(40L, OTHER_INSTRUCTOR_ID))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(courseRepository, never()).delete(any(Course.class));
        verifyNoInteractions(events);
    }

    @Test
    void delete_missingCourse_returns404() {
        given(courseRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.delete(404L, INSTRUCTOR_ID))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
