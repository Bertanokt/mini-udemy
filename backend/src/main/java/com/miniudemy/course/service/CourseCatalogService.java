package com.miniudemy.course.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.CourseDetailResponse;
import com.miniudemy.course.dto.CourseStats;
import com.miniudemy.course.dto.CourseSummaryPage;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.dto.SectionResponse;
import com.miniudemy.course.mapper.CourseMapper;
import com.miniudemy.course.repository.CourseRepository;
import com.miniudemy.course.repository.LessonRepository;
import com.miniudemy.course.repository.LessonRepository.CourseTotals;
import com.miniudemy.course.repository.SectionRepository;

/**
 * Herkese açık katalog ve kurs detayı (#26).
 *
 * Sorgu sayısı sayfa boyutundan bağımsızdır: kurslar (kategori ile) bir sorgu, ders sayısı/süresi bir sorgu, eğitmen
 * adları bir sorgu, kayıt sayıları bir sorgu. Detayda bölümler ve dersleri tek sorguda gelir.
 */
@Service
@Transactional(readOnly = true)
public class CourseCatalogService {

    private static final String COURSE_NOT_FOUND = "Kurs bulunamadı.";

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final UserDirectoryService userDirectory;
    private final EnrollmentCounts enrollmentCounts;
    private final CourseMapper mapper;

    public CourseCatalogService(CourseRepository courseRepository, SectionRepository sectionRepository,
            LessonRepository lessonRepository, UserDirectoryService userDirectory,
            EnrollmentCounts enrollmentCounts, CourseMapper mapper) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.userDirectory = userDirectory;
        this.enrollmentCounts = enrollmentCounts;
        this.mapper = mapper;
    }

    /**
     * Yayımlanmış kurslarda arama. Filtreler VE ile birleşir; {@code q} başlık ve açıklamada büyük/küçük harf duyarsız.
     *
     * {@code minRating} şimdilik yok sayılır: puan verisi review modülündedir (#126, #143); modül gelince bu filtre ve
     * {@code sort=rating} burada devreye girer.
     */
    public CourseSummaryPage search(String q, Long categoryId, CourseLevel level, Long instructorId,
            Double minRating, CatalogSort sort, int page, int size) {
        String pattern = toLikePattern(q);
        if (sort == CatalogSort.POPULAR) {
            return searchByPopularity(pattern, categoryId, level, instructorId, page, size);
        }
        Page<Course> result = courseRepository.searchCatalog(pattern, categoryId, level, instructorId,
                PageRequest.of(page, size, sort.sorting()));
        Map<Long, Long> counts = enrollmentCounts.countByCourses(ids(result.getContent()));
        return new CourseSummaryPage(toCards(result.getContent(), counts), page, size, result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Yönetici listesi (#59): verilen durumdaki kurslar (durum yoksa hepsi), en uzun süredir değişmeyen önce; onay
     * kuyruğu için {@code IN_REVIEW}. Kartlar katalogdakiyle aynı.
     */
    public CourseSummaryPage listByStatus(CourseStatus status, int page, int size) {
        Page<Course> result = courseRepository.findAllByOptionalStatus(status,
                PageRequest.of(page, size, Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id"))));
        Map<Long, Long> counts = enrollmentCounts.countByCourses(ids(result.getContent()));
        return new CourseSummaryPage(toCards(result.getContent(), counts), page, size, result.getTotalElements(),
                result.getTotalPages());
    }

    /** Kurs detayı. Yayımlanmamış kurs yalnız sahibine ve yöneticiye görünür; diğerlerine varlığı açığa çıkmadan 404. */
    public CourseDetailResponse getDetail(Long courseId, Long viewerId, boolean viewerIsAdmin) {
        Course course = courseRepository.findWithCategoryById(courseId)
                .orElseThrow(() -> new NotFoundException(COURSE_NOT_FOUND));
        boolean owner = viewerId != null && viewerId.equals(course.getInstructorId());
        if (!course.isPublished() && !owner && !viewerIsAdmin) {
            throw new NotFoundException(COURSE_NOT_FOUND);
        }

        List<Section> sections = sectionRepository.findAllWithLessonsByCourseId(courseId);
        int lessonCount = sections.stream().mapToInt(s -> s.getLessons().size()).sum();
        int totalDuration = sections.stream().mapToInt(Section::getTotalDurationSeconds).sum();
        CourseStats stats = stats(lessonCount, totalDuration, enrollmentCounts.countByCourse(courseId));
        List<SectionResponse> sectionResponses = sections.stream().map(mapper::toSectionResponse).toList();
        return mapper.toDetail(course, instructorOf(course.getInstructorId()), stats, sectionResponses);
    }

    /**
     * Kayıt sayısına göre sıralama: sayı başka modülde olduğundan eşleşen kursların id'leri ve sayıları alınır, sıra
     * bellekte kurulur, yalnız istenen sayfanın kursları yüklenir. Katalog küçük (yüzlerce kurs) olduğu için kabul
     * edilir; büyürse sayı kursta tutulan bir kolona taşınır.
     */
    private CourseSummaryPage searchByPopularity(String pattern, Long categoryId, CourseLevel level,
            Long instructorId, int page, int size) {
        List<Long> matching = courseRepository.searchCatalogIds(pattern, categoryId, level, instructorId);
        Map<Long, Long> counts = enrollmentCounts.countByCourses(matching);
        List<Long> ranked = matching.stream()
                .sorted(Comparator.comparing((Long id) -> counts.getOrDefault(id, 0L)).reversed()
                        .thenComparing(Comparator.reverseOrder()))
                .toList();

        long from = (long) page * size;
        List<Long> pageIds = from >= ranked.size() ? List.of()
                : ranked.subList((int) from, (int) Math.min(from + size, ranked.size()));
        Map<Long, Course> byId = new HashMap<>();
        if (!pageIds.isEmpty()) {
            courseRepository.findAllByIdIn(pageIds).forEach(c -> byId.put(c.getId(), c));
        }
        List<Course> ordered = pageIds.stream().map(byId::get).toList();
        int totalPages = (int) ((ranked.size() + size - 1L) / size);
        return new CourseSummaryPage(toCards(ordered, counts), page, size, ranked.size(), totalPages);
    }

    /** Verilen kurslar için kart: sayılar ve eğitmen adları toplu sorgularla gelir (CourseLookupService kullanır). */
    List<CourseSummaryResponse> summaries(List<Course> courses) {
        return toCards(courses, enrollmentCounts.countByCourses(ids(courses)));
    }

    /** Sayfadaki kurslar için ders sayıları, eğitmen adları ve (hazır) kayıt sayıları; her biri tek sorgu. */
    private List<CourseSummaryResponse> toCards(List<Course> courses, Map<Long, Long> enrollmentByCourse) {
        if (courses.isEmpty()) {
            return List.of();
        }
        Map<Long, CourseTotals> totals = new HashMap<>();
        lessonRepository.totalsByCourseIds(ids(courses)).forEach(t -> totals.put(t.getCourseId(), t));
        Map<Long, UserSummary> instructors = userDirectory.findSummaries(new LinkedHashSet<>(
                courses.stream().map(Course::getInstructorId).toList()));

        return courses.stream().map(course -> {
            CourseTotals t = totals.get(course.getId());
            CourseStats stats = stats(t == null ? 0 : (int) t.getLessonCount(),
                    t == null ? 0 : (int) t.getTotalDurationSeconds(),
                    enrollmentByCourse.getOrDefault(course.getId(), 0L));
            return mapper.toSummary(course, instructors.getOrDefault(course.getInstructorId(),
                    unknownInstructor(course.getInstructorId())), stats);
        }).toList();
    }

    private UserSummary instructorOf(Long instructorId) {
        return userDirectory.findSummaries(List.of(instructorId))
                .getOrDefault(instructorId, unknownInstructor(instructorId));
    }

    private static UserSummary unknownInstructor(Long id) {
        return new UserSummary(id, "Eğitmen");
    }

    /** Puan alanları review modülü gelene dek null / 0 (#126, #143). */
    private static CourseStats stats(int lessonCount, int totalDurationSeconds, long enrollmentCount) {
        return new CourseStats(lessonCount, totalDurationSeconds, enrollmentCount, null, 0);
    }

    private static List<Long> ids(Collection<Course> courses) {
        return courses.stream().map(Course::getId).toList();
    }

    /** `like` ifadesi: küçük harf, `\`, `%`, `_` kaçırılmış, `%` ile sarılmış; boş arama her şeyle eşleşir. */
    static String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String escaped = q.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
