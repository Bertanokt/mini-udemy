package com.miniudemy.course.service;

import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.CourseDetailResponse;
import com.miniudemy.course.dto.CourseSummaryPage;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.mapper.CategoryMapperImpl;
import com.miniudemy.course.mapper.CourseMapperImpl;
import com.miniudemy.course.repository.CourseRepository;
import com.miniudemy.course.repository.LessonRepository;
import com.miniudemy.course.repository.LessonRepository.CourseTotals;
import com.miniudemy.course.repository.SectionRepository;

/** Katalog araması ve kurs detayı (#26): filtreler, sayfalama, sıralama, görünürlük ve sayılar. */
class CourseCatalogServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final SectionRepository sectionRepository = mock(SectionRepository.class);
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final EnrollmentCounts enrollmentCounts = mock(EnrollmentCounts.class);
    private final CourseRatings courseRatings = mock(CourseRatings.class);
    private final CourseRoster roster = mock(CourseRoster.class);
    private final CourseCatalogService service = new CourseCatalogService(courseRepository, sectionRepository,
            lessonRepository, userDirectory, enrollmentCounts, courseRatings, new CourseMapperImpl(new CategoryMapperImpl()),
            new CourseVisibility(roster));

    // --- yönetici listesi (#59) ---

    @Test
    void listByStatus_passesStatusPageAndOldestChangedFirstOrder_andBuildsCards() {
        Course course = CourseFixtures.course(5L, 7L, "Bekleyen kurs", CourseStatus.IN_REVIEW);
        given(courseRepository.findAllByOptionalStatus(eq(CourseStatus.IN_REVIEW), any()))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(course),
                        PageRequest.of(1, 10), 11));
        given(userDirectory.findSummaries(any())).willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Kaya")));

        CourseSummaryPage result = service.listByStatus(CourseStatus.IN_REVIEW, 1, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).findAllByOptionalStatus(eq(CourseStatus.IN_REVIEW), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id")));
        assertThat(result.content()).extracting(CourseSummaryResponse::id).containsExactly(5L);
        assertThat(result.content().get(0).status()).isEqualTo(CourseStatus.IN_REVIEW);
        assertThat(result.totalElements()).isEqualTo(11);
    }

    @Test
    void listByStatus_withoutStatus_listsAllStatuses() {
        given(courseRepository.findAllByOptionalStatus(any(), any())).willReturn(Page.empty());

        service.listByStatus(null, 0, 20);

        verify(courseRepository).findAllByOptionalStatus(eq(null), any());
    }

    // --- arama ---

    @Test
    void search_passesFiltersAndPageToRepository() {
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any())).willReturn(Page.empty());

        service.search("Java", 3L, CourseLevel.ADVANCED, 9L, null, CatalogSort.NEWEST, 2, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).searchCatalog(eq("%java%"), eq(3L), eq(CourseLevel.ADVANCED), eq(9L), eq(false),
                any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void search_withoutFilters_matchesEverything() {
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any())).willReturn(Page.empty());

        service.search(null, null, null, null, null, CatalogSort.NEWEST, 0, 20);

        verify(courseRepository).searchCatalog(eq("%"), eq(null), eq(null), eq(null), eq(false), any(), any());
        verify(courseRatings, never()).courseIdsWithAverageAtLeast(anyDouble());
    }

    @Test
    void search_escapesLikeWildcardsAndLowercasesKeyword() {
        assertThat(CourseCatalogService.toLikePattern("  100%_Java\\ ")).isEqualTo("%100\\%\\_java\\\\%");
        assertThat(CourseCatalogService.toLikePattern("   ")).isEqualTo("%");
    }

    @Test
    void search_newestSortsByCreatedAtDescendingWithIdTieBreaker() {
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any())).willReturn(Page.empty());

        service.search(null, null, null, null, null, CatalogSort.NEWEST, 0, 20);

        assertThat(capturedSort()).isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    void search_titleSortsAlphabeticallyIgnoringCase() {
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any())).willReturn(Page.empty());

        service.search(null, null, null, null, null, CatalogSort.TITLE, 0, 20);

        assertThat(capturedSort()).isEqualTo(Sort.by(Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id")));
    }

    @Test
    void search_ratingSortIsOutOfScopeAndFallsBackToNewest() {
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .willReturn(Page.empty());

        service.search(null, null, null, null, null, CatalogSort.RATING, 0, 20);

        assertThat(capturedSort()).isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    void search_minRating_restrictsQueryToCoursesMeetingTheAverageAndKeepsFilteredTotal() {
        given(courseRatings.courseIdsWithAverageAtLeast(4.0)).willReturn(Set.of(1L, 3L));
        Course one = CourseFixtures.course(1, 7, "Bir", CourseStatus.PUBLISHED);
        Course three = CourseFixtures.course(3, 7, "Üç", CourseStatus.PUBLISHED);
        given(courseRepository.searchCatalog(any(), any(), any(), any(), eq(true), eq(Set.of(1L, 3L)), any()))
                .willReturn(new PageImpl<>(List.of(three, one), PageRequest.of(0, 2), 2));
        given(userDirectory.findSummaries(anyCollection())).willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Demir")));
        given(enrollmentCounts.countByCourses(anyCollection())).willReturn(Map.of());
        given(courseRatings.ratingsByCourses(anyCollection()))
                .willReturn(Map.of(1L, new CourseRating(4.0, 2), 3L, new CourseRating(4.8, 5)));

        CourseSummaryPage page = service.search(null, null, null, null, 4.0, CatalogSort.NEWEST, 0, 2);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(CourseSummaryResponse::averageRating).containsExactly(4.8, 4.0);
        assertThat(page.content()).extracting(CourseSummaryResponse::reviewCount).containsExactly(5, 2);
    }

    @Test
    void search_minRatingMatchesNoCourse_returnsEmptyPageWithoutCatalogQuery() {
        given(courseRatings.courseIdsWithAverageAtLeast(5.0)).willReturn(Set.of());

        CourseSummaryPage page = service.search(null, null, null, null, 5.0, CatalogSort.NEWEST, 0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        verifyNoInteractions(courseRepository, lessonRepository, userDirectory);
    }

    @Test
    void search_minRatingWithPopularSort_passesRestrictionToIdQueryAndKeepsFilteredTotal() {
        given(courseRatings.courseIdsWithAverageAtLeast(3.0)).willReturn(Set.of(1L, 2L));
        given(courseRepository.searchCatalogIds(any(), any(), any(), any(), eq(true), eq(Set.of(1L, 2L))))
                .willReturn(List.of(1L, 2L));
        given(enrollmentCounts.countByCourses(anyCollection())).willReturn(Map.of(1L, 3L, 2L, 9L));
        given(courseRepository.findAllByIdIn(anyCollection())).willReturn(List.of(
                CourseFixtures.course(1, 7, "Bir", CourseStatus.PUBLISHED),
                CourseFixtures.course(2, 7, "İki", CourseStatus.PUBLISHED)));
        given(userDirectory.findSummaries(anyCollection())).willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Demir")));
        given(courseRatings.ratingsByCourses(anyCollection()))
                .willReturn(Map.of(1L, new CourseRating(3.0, 1), 2L, new CourseRating(4.5, 2)));

        CourseSummaryPage page = service.search(null, null, null, null, 3.0, CatalogSort.POPULAR, 0, 20);

        assertThat(page.content()).extracting(CourseSummaryResponse::id).containsExactly(2L, 1L);
        assertThat(page.content()).extracting(CourseSummaryResponse::averageRating).containsExactly(4.5, 3.0);
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void search_courseWithoutReviews_hasNullAverageAndZeroCount() {
        Course course = CourseFixtures.course(1, 7, "Bir", CourseStatus.PUBLISHED);
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .willReturn(new PageImpl<>(List.of(course)));
        given(userDirectory.findSummaries(anyCollection())).willReturn(Map.of());
        given(enrollmentCounts.countByCourses(anyCollection())).willReturn(Map.of());
        given(courseRatings.ratingsByCourses(anyCollection())).willReturn(Map.of(1L, new CourseRating(null, 0)));

        CourseSummaryResponse card = service.search(null, null, null, null, null, CatalogSort.NEWEST, 0, 20)
                .content().get(0);

        assertThat(card.averageRating()).isNull();
        assertThat(card.reviewCount()).isZero();
    }

    @Test
    void search_buildsCardsWithInstructorLessonTotalsAndEnrollmentCounts() {
        Course java = CourseFixtures.course(1, 7, "Java", CourseStatus.PUBLISHED);
        Course empty = CourseFixtures.course(2, 8, "Boş kurs", CourseStatus.PUBLISHED);
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .willReturn(new PageImpl<>(List.of(java, empty), org.springframework.data.domain.PageRequest.of(0, 2), 5));
        CourseTotals javaTotals = totals(1, 4, 1200);
        given(lessonRepository.totalsByCourseIds(anyCollection())).willReturn(List.of(javaTotals));
        given(userDirectory.findSummaries(anyCollection()))
                .willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Demir"), 8L, new UserSummary(8L, "Ayşe Kaya")));
        given(enrollmentCounts.countByCourses(anyCollection())).willReturn(Map.of(1L, 30L));
        given(courseRatings.ratingsByCourses(anyCollection())).willReturn(Map.of(1L, new CourseRating(4.6, 12)));

        CourseSummaryPage page = service.search(null, null, null, null, null, CatalogSort.NEWEST, 0, 2);

        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        CourseSummaryResponse first = page.content().get(0);
        assertThat(first.instructor().fullName()).isEqualTo("Mehmet Demir");
        assertThat(first.lessonCount()).isEqualTo(4);
        assertThat(first.totalDurationSeconds()).isEqualTo(1200);
        assertThat(first.enrollmentCount()).isEqualTo(30L);
        assertThat(first.averageRating()).isEqualTo(4.6);
        assertThat(first.reviewCount()).isEqualTo(12);
        CourseSummaryResponse second = page.content().get(1);
        assertThat(second.lessonCount()).isZero();
        assertThat(second.totalDurationSeconds()).isZero();
        assertThat(second.enrollmentCount()).isZero();
    }

    @Test
    void search_usesOneQueryPerKindRegardlessOfPageSize() {
        List<Course> courses = List.of(
                CourseFixtures.course(1, 7, "A", CourseStatus.PUBLISHED),
                CourseFixtures.course(2, 7, "B", CourseStatus.PUBLISHED),
                CourseFixtures.course(3, 8, "C", CourseStatus.PUBLISHED));
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any())).willReturn(new PageImpl<>(courses));
        given(userDirectory.findSummaries(anyCollection())).willReturn(Map.of());
        given(enrollmentCounts.countByCourses(anyCollection())).willReturn(Map.of());

        service.search(null, null, null, null, null, CatalogSort.NEWEST, 0, 20);

        verify(lessonRepository).totalsByCourseIds(List.of(1L, 2L, 3L));
        verify(userDirectory).findSummaries(java.util.Set.of(7L, 8L));
        verify(enrollmentCounts).countByCourses(List.of(1L, 2L, 3L));
        verify(courseRatings).ratingsByCourses(List.of(1L, 2L, 3L));
    }

    @Test
    void search_noResults_returnsEmptyPageWithoutExtraQueries() {
        given(courseRepository.searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .willReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));
        given(enrollmentCounts.countByCourses(anyCollection())).willReturn(Map.of());

        CourseSummaryPage page = service.search("yok", null, null, null, null, CatalogSort.NEWEST, 0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        verifyNoInteractions(lessonRepository, userDirectory);
    }

    @Test
    void search_popularOrdersByEnrollmentCountThenNewestAndSlicesPage() {
        given(courseRepository.searchCatalogIds(any(), any(), any(), any(), anyBoolean(), any())).willReturn(List.of(1L, 2L, 3L, 4L, 5L));
        given(enrollmentCounts.countByCourses(anyCollection()))
                .willReturn(Map.of(1L, 5L, 2L, 50L, 3L, 5L, 4L, 0L, 5L, 20L));
        // Sıra: 2 (50), 5 (20), 3 (5; id büyük önce), 1 (5), 4 (0). Sayfa 1, boyut 2 → 3 ve 1.
        given(courseRepository.findAllByIdIn(anyCollection())).willReturn(List.of(
                CourseFixtures.course(1, 7, "Bir", CourseStatus.PUBLISHED),
                CourseFixtures.course(3, 7, "Üç", CourseStatus.PUBLISHED)));
        given(userDirectory.findSummaries(anyCollection())).willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Demir")));

        CourseSummaryPage page = service.search(null, null, null, null, null, CatalogSort.POPULAR, 1, 2);

        assertThat(page.content()).extracting(CourseSummaryResponse::id).containsExactly(3L, 1L);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content()).extracting(CourseSummaryResponse::enrollmentCount).containsExactly(5L, 5L);
        verify(courseRepository).findAllByIdIn(List.of(3L, 1L));
    }

    @Test
    void search_popularPageBeyondEnd_returnsEmptyContentWithTotals() {
        given(courseRepository.searchCatalogIds(any(), any(), any(), any(), anyBoolean(), any())).willReturn(List.of(1L, 2L));
        given(enrollmentCounts.countByCourses(anyCollection())).willReturn(Map.of(1L, 1L, 2L, 2L));

        CourseSummaryPage page = service.search(null, null, null, null, null, CatalogSort.POPULAR, 4, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void catalogSortFromValue_defaultsToNewestAndRejectsUnknown() {
        assertThat(CatalogSort.fromValue(null)).isEqualTo(CatalogSort.NEWEST);
        assertThat(CatalogSort.fromValue("popular")).isEqualTo(CatalogSort.POPULAR);
        assertThatThrownBy(() -> CatalogSort.fromValue("Popular")).isInstanceOf(IllegalArgumentException.class);
    }

    // --- detay ---

    @Test
    void getDetail_published_returnsSectionsInOrderWithTotalsAndInstructor() {
        Course course = CourseFixtures.course(5, 7, "Java", CourseStatus.PUBLISHED);
        Section first = CourseFixtures.section(course, 10, "Giriş", 0);
        CourseFixtures.lesson(first, 20, "Kurulum", 300, 0, 55L);
        CourseFixtures.lesson(first, 21, "Merhaba dünya", 200, 1, null);
        Section second = CourseFixtures.section(course, 11, "Sınıflar", 1);
        CourseFixtures.lesson(second, 22, "Nesneler", 500, 0, 56L);
        given(courseRepository.findWithCategoryById(5L)).willReturn(Optional.of(course));
        given(sectionRepository.findAllWithLessonsByCourseId(5L)).willReturn(List.of(first, second));
        given(userDirectory.findSummaries(anyCollection())).willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Demir")));
        given(enrollmentCounts.countByCourse(5L)).willReturn(41L);
        given(courseRatings.ratingOf(5L)).willReturn(new CourseRating(4.2, 7));

        CourseDetailResponse detail = service.getDetail(5L, null, false);

        assertThat(detail.sections()).extracting(s -> s.title()).containsExactly("Giriş", "Sınıflar");
        assertThat(detail.sections().get(0).lessons()).extracting(l -> l.title())
                .containsExactly("Kurulum", "Merhaba dünya");
        assertThat(detail.sections().get(0).lessons()).extracting(l -> l.hasContent()).containsExactly(true, false);
        assertThat(detail.lessonCount()).isEqualTo(3);
        assertThat(detail.totalDurationSeconds()).isEqualTo(1000);
        assertThat(detail.enrollmentCount()).isEqualTo(41L);
        assertThat(detail.instructor().fullName()).isEqualTo("Mehmet Demir");
        assertThat(detail.learningOutcomes()).containsExactly("Hedef 1", "Hedef 2");
        assertThat(detail.averageRating()).isEqualTo(4.2);
        assertThat(detail.reviewCount()).isEqualTo(7);
    }

    @Test
    void getDetail_unknownCourse_throwsNotFound() {
        given(courseRepository.findWithCategoryById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(404L, null, false)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getDetail_unpublishedForAnonymous_throwsNotFound() {
        stubCourse(CourseStatus.IN_REVIEW);

        assertThatThrownBy(() -> service.getDetail(5L, null, false)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getDetail_unpublishedForOtherUser_throwsNotFoundLikeMissingCourse() {
        stubCourse(CourseStatus.DRAFT);

        assertThatThrownBy(() -> service.getDetail(5L, 99L, false))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Kurs bulunamadı.");
    }

    @Test
    void getDetail_unpublishedForOwner_returnsDetail() {
        stubCourse(CourseStatus.DRAFT);

        assertThat(service.getDetail(5L, 7L, false).status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void getDetail_unpublishedForAdmin_returnsDetail() {
        stubCourse(CourseStatus.UNPUBLISHED);

        assertThat(service.getDetail(5L, 1L, true).status()).isEqualTo(CourseStatus.UNPUBLISHED);
    }

    /** #123: yayından kaldırılan kursa önceden kaydolan kursiyer detayı görmeye devam eder; kayıtsız kullanıcı 404. */
    @Test
    void getDetail_unpublishedCourse_isVisibleToEnrolledStudent_butNotToOthers() {
        stubCourse(CourseStatus.UNPUBLISHED);
        given(roster.isEnrolled(5L, 42L)).willReturn(true);

        assertThat(service.getDetail(5L, 42L, false).status()).isEqualTo(CourseStatus.UNPUBLISHED);
        assertThatThrownBy(() -> service.getDetail(5L, 43L, false)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getDetail_unpublishedNotFound_doesNotLoadSections() {
        stubCourse(CourseStatus.DRAFT);

        assertThatThrownBy(() -> service.getDetail(5L, null, false)).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(sectionRepository, userDirectory, enrollmentCounts, courseRatings);
    }

    private void stubCourse(CourseStatus status) {
        Course course = CourseFixtures.course(5, 7, "Java", status);
        given(courseRepository.findWithCategoryById(5L)).willReturn(Optional.of(course));
        given(sectionRepository.findAllWithLessonsByCourseId(5L)).willReturn(List.of());
        given(courseRatings.ratingOf(5L)).willReturn(new CourseRating(null, 0));
        given(userDirectory.findSummaries(anyCollection())).willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Demir")));
    }

    private Sort capturedSort() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).searchCatalog(any(), any(), any(), any(), anyBoolean(), any(), pageable.capture());
        return pageable.getValue().getSort();
    }

    private static CourseTotals totals(long courseId, long lessonCount, long duration) {
        CourseTotals totals = mock(CourseTotals.class);
        given(totals.getCourseId()).willReturn(courseId);
        given(totals.getLessonCount()).willReturn(lessonCount);
        given(totals.getTotalDurationSeconds()).willReturn(duration);
        return totals;
    }
}
