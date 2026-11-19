package com.miniudemy.course.service;

import com.miniudemy.course.domain.ContentFileDetached;
import static com.miniudemy.course.CourseFixtures.course;
import static com.miniudemy.course.CourseFixtures.lesson;
import static com.miniudemy.course.CourseFixtures.section;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLessonsChanged;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.SectionRequest;
import com.miniudemy.course.dto.SectionResponse;
import com.miniudemy.course.mapper.CategoryMapperImpl;
import com.miniudemy.course.mapper.CourseMapperImpl;
import com.miniudemy.course.repository.CourseRepository;
import com.miniudemy.course.repository.SectionRepository;

/** Bölüm işlemleri (#24). Veritabanı taklit; sahiplik servisi ve mapper gerçek. */
class SectionServiceTest {

    private static final long OWNER = 7L;
    private static final long OTHER_INSTRUCTOR = 8L;

    private final SectionRepository sectionRepository = mock(SectionRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CourseRoster roster = mock(CourseRoster.class);
    private SectionService service;

    @BeforeEach
    void setUp() {
        CourseOwnershipService ownership = new CourseOwnershipService(courseRepository, sectionRepository);
        service = new SectionService(sectionRepository, courseRepository, ownership,
                new CourseMapperImpl(new CategoryMapperImpl()), events, new CourseVisibility(roster));
        given(courseRepository.findById(99L)).willReturn(Optional.empty());
        given(sectionRepository.findById(999L)).willReturn(Optional.empty());
    }

    /** Kursu ve bölümlerini depo taklitlerine tanıtır. */
    private Course stored(Course course) {
        given(courseRepository.findById(course.getId())).willReturn(Optional.of(course));
        given(sectionRepository.findAllWithLessonsByCourseId(course.getId())).willReturn(course.getSections());
        for (Section s : course.getSections()) {
            given(sectionRepository.findById(s.getId())).willReturn(Optional.of(s));
        }
        return course;
    }

    private static List<String> titles(List<SectionResponse> sections) {
        return sections.stream().map(SectionResponse::title).toList();
    }

    private static List<Integer> positions(List<SectionResponse> sections) {
        return sections.stream().map(SectionResponse::position).toList();
    }

    private static void assertStatus(Throwable ex, HttpStatus status) {
        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex).getStatus()).isEqualTo(status);
    }

    private Course draftWithThreeSections() {
        Course course = course(1L, OWNER, "Sıfırdan Java", CourseStatus.DRAFT);
        section(course, 11L, "Giriş", 0);
        section(course, 12L, "Nesneler", 1);
        section(course, 13L, "Koleksiyonlar", 2);
        return stored(course);
    }

    // --- Listeleme ---

    @Test
    void listSections_publishedCourse_isVisibleWithoutLogin_inPositionOrderWithLessons() {
        Course course = course(1L, OWNER, "Sıfırdan Java", CourseStatus.PUBLISHED);
        Section intro = section(course, 11L, "Giriş", 0);
        lesson(intro, 101L, "Kurulum", 300, 0, 5L);
        section(course, 12L, "Nesneler", 1);
        stored(course);

        List<SectionResponse> sections = service.listSections(1L, null, false);

        assertThat(titles(sections)).containsExactly("Giriş", "Nesneler");
        assertThat(sections.get(0).courseId()).isEqualTo(1L);
        assertThat(sections.get(0).lessons()).hasSize(1);
    }

    @Test
    void listSections_draftCourse_isHiddenFromOthers_butVisibleToOwnerAndAdmin() {
        stored(course(1L, OWNER, "Taslak kurs", CourseStatus.DRAFT));

        assertThatThrownBy(() -> service.listSections(1L, null, false))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.listSections(1L, OTHER_INSTRUCTOR, false))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
        assertThat(service.listSections(1L, OWNER, false)).isEmpty();
        assertThat(service.listSections(1L, 1L, true)).isEmpty();
    }

    /** #123: yayından kaldırılan kursun dersleri, önceden kaydolan kursiyerin oynatıcısında açılmaya devam eder. */
    @Test
    void listSections_unpublishedCourse_isVisibleToEnrolledStudent_butNotToOthers() {
        stored(course(1L, OWNER, "Kaldırılan kurs", CourseStatus.UNPUBLISHED));
        given(roster.isEnrolled(1L, 42L)).willReturn(true);

        assertThat(service.listSections(1L, 42L, false)).isEmpty();
        assertThatThrownBy(() -> service.listSections(1L, 43L, false))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.listSections(1L, null, false))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    @Test
    void listSections_unknownCourse_returns404() {
        assertThatThrownBy(() -> service.listSections(99L, OWNER, false))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    // --- Ekleme ---

    @Test
    void createSection_appendsAtTheEnd_withPositionEqualToSectionCount() {
        draftWithThreeSections();

        SectionResponse created = service.createSection(1L, OWNER, new SectionRequest("Akışlar"));

        assertThat(created.title()).isEqualTo("Akışlar");
        assertThat(created.position()).isEqualTo(3);
        assertThat(created.courseId()).isEqualTo(1L);
        assertThat(created.lessons()).isEmpty();
        verify(sectionRepository).saveAndFlush(any(Section.class));
    }

    @Test
    void createSection_onEmptyCourse_startsAtPositionZero() {
        stored(course(1L, OWNER, "Boş kurs", CourseStatus.DRAFT));

        assertThat(service.createSection(1L, OWNER, new SectionRequest("Giriş")).position()).isZero();
    }

    @Test
    void createSection_byAnotherInstructor_returns403_andSavesNothing() {
        draftWithThreeSections();

        assertThatThrownBy(() -> service.createSection(1L, OTHER_INSTRUCTOR, new SectionRequest("Akışlar")))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        verify(sectionRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSection_whenCourseIsInReview_returns409_andSavesNothing() {
        stored(course(1L, OWNER, "İncelemedeki kurs", CourseStatus.IN_REVIEW));

        assertThatThrownBy(() -> service.createSection(1L, OWNER, new SectionRequest("Akışlar")))
                .satisfies(ex -> assertStatus(ex, HttpStatus.CONFLICT));
        verify(sectionRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSection_unknownCourse_returns404() {
        assertThatThrownBy(() -> service.createSection(99L, OWNER, new SectionRequest("Akışlar")))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    // --- Yeniden adlandırma ---

    @Test
    void updateSection_renamesAndKeepsPosition() {
        draftWithThreeSections();

        SectionResponse updated = service.updateSection(12L, OWNER, new SectionRequest("Sınıflar ve nesneler"));

        assertThat(updated.title()).isEqualTo("Sınıflar ve nesneler");
        assertThat(updated.position()).isEqualTo(1);
    }

    @Test
    void updateSection_byAnotherInstructor_returns403_andKeepsTitle() {
        Course course = draftWithThreeSections();

        assertThatThrownBy(() -> service.updateSection(12L, OTHER_INSTRUCTOR, new SectionRequest("Değişti")))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        assertThat(course.getSections().get(1).getTitle()).isEqualTo("Nesneler");
    }

    @Test
    void updateSection_unknownSection_returns404() {
        assertThatThrownBy(() -> service.updateSection(999L, OWNER, new SectionRequest("Değişti")))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    // --- Silme ---

    @Test
    void deleteSection_removesIt_andRenumbersTheRestWithoutGaps() {
        Course course = draftWithThreeSections();

        service.deleteSection(12L, OWNER);

        assertThat(course.getSections()).extracting(Section::getTitle).containsExactly("Giriş", "Koleksiyonlar");
        assertThat(course.getSections()).extracting(Section::getPosition).containsExactly(0, 1);
    }

    @Test
    void deleteSection_whenCourseIsPublished_returns409_andKeepsIt() {
        Course course = course(1L, OWNER, "Yayımlanmış kurs", CourseStatus.PUBLISHED);
        section(course, 11L, "Giriş", 0);
        stored(course);

        assertThatThrownBy(() -> service.deleteSection(11L, OWNER))
                .satisfies(ex -> assertStatus(ex, HttpStatus.CONFLICT));
        assertThat(course.getSections()).hasSize(1);
    }

    // --- Sıra değiştirme ---

    @Test
    void moveSection_toLaterPosition_shiftsTheOthersUp() {
        draftWithThreeSections();

        List<SectionResponse> sections = service.moveSection(11L, OWNER, 2);

        assertThat(titles(sections)).containsExactly("Nesneler", "Koleksiyonlar", "Giriş");
        assertThat(positions(sections)).containsExactly(0, 1, 2);
    }

    @Test
    void moveSection_toEarlierPosition_shiftsTheOthersDown() {
        draftWithThreeSections();

        List<SectionResponse> sections = service.moveSection(13L, OWNER, 0);

        assertThat(titles(sections)).containsExactly("Koleksiyonlar", "Giriş", "Nesneler");
        assertThat(positions(sections)).containsExactly(0, 1, 2);
    }

    @Test
    void moveSection_positionBeyondLastSection_returns400OnPositionField_andKeepsOrder() {
        Course course = draftWithThreeSections();

        assertThatThrownBy(() -> service.moveSection(11L, OWNER, 3))
                .satisfies(ex -> {
                    assertStatus(ex, HttpStatus.BAD_REQUEST);
                    assertThat(((ApiException) ex).getErrors())
                            .isEqualTo(Map.of("position", "Sıra 0 ile 2 arasında olmalıdır."));
                });
        assertThat(course.getSections()).extracting(Section::getTitle)
                .containsExactly("Giriş", "Nesneler", "Koleksiyonlar");
    }

    @Test
    void moveSection_byAnotherInstructor_returns403() {
        draftWithThreeSections();

        assertThatThrownBy(() -> service.moveSection(11L, OTHER_INSTRUCTOR, 2))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
    }

    // ders sayısı (#249)

    @Test
    void deleteSection_publishesCourseLessonsChanged_butCreateRenameAndMoveDoNot() {
        Course course = CourseFixtures.course(5L, 7L, "Kurs", CourseStatus.DRAFT);
        Section first = CourseFixtures.section(course, 1L, "Giriş", 0);
        CourseFixtures.lesson(first, 11L, "Kurulum", 60, 0, null);
        Section second = CourseFixtures.section(course, 2L, "Güvenlik", 1);
        given(courseRepository.findById(5L)).willReturn(Optional.of(course));
        given(sectionRepository.findById(1L)).willReturn(Optional.of(first));
        given(sectionRepository.findById(2L)).willReturn(Optional.of(second));
        given(sectionRepository.saveAndFlush(any(Section.class))).willAnswer(call -> call.getArgument(0));

        service.createSection(5L, 7L, new SectionRequest("Test"));
        service.updateSection(2L, 7L, new SectionRequest("Güvenlik ve yetki"));
        service.moveSection(2L, 7L, 0);
        verify(events, never()).publishEvent(any(Object.class));

        service.deleteSection(1L, 7L);
        verify(events).publishEvent(new CourseLessonsChanged(5L));
    }

    // dosya bağı (#112)

    @Test
    void deleteSection_publishesContentFileDetachedForEveryLessonFile() {
        Course course = CourseFixtures.course(5L, 7L, "Kurs", CourseStatus.DRAFT);
        Section section = CourseFixtures.section(course, 1L, "Giriş", 0);
        CourseFixtures.lesson(section, 11L, "Bir", 60, 0, 900L);
        CourseFixtures.lesson(section, 12L, "İki", 60, 1, null);
        CourseFixtures.lesson(section, 13L, "Üç", 60, 2, 901L);
        given(courseRepository.findById(5L)).willReturn(Optional.of(course));
        given(sectionRepository.findById(1L)).willReturn(Optional.of(section));

        service.deleteSection(1L, 7L);

        verify(events).publishEvent(new ContentFileDetached(900L));
        verify(events).publishEvent(new ContentFileDetached(901L));
        verify(events, never()).publishEvent(new ContentFileDetached(null));
    }
}
