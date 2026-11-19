package com.miniudemy.course.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Category;
import com.miniudemy.course.domain.ContentFileDetached;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CreateCourseRequest;
import com.miniudemy.course.dto.UpdateCourseRequest;
import com.miniudemy.course.mapper.CourseMapper;
import com.miniudemy.course.repository.CategoryRepository;
import com.miniudemy.course.repository.CourseRepository;

/**
 * Kurs işlemleri: oluşturma (#22, FR-07), düzenleme ve silme (#23); durum geçişleri {@link CourseLifecycleService}'te.
 *
 * <ul>
 *   <li>Eğitmen, istekten değil giriş yapan kullanıcıdan gelir: controller {@code CurrentUser.id(...)}'yi
 *       {@code instructorId} olarak verir (docs/architecture.md › Yetki kontrolleri).</li>
 *   <li>Kurs {@code DRAFT} durumunda başlar; başlangıç durumunu entity belirler.</li>
 *   <li>Kategori yoksa 400 ve {@code errors.categoryId} (sözleşme › CreateCourseRequest).</li>
 *   <li>Eğitmenin adı {@code auth} modülünün public servisinden ({@link UserDirectoryService}) alınır.</li>
 * </ul>
 */
@Service
public class CourseService {

    static final String CATEGORY_NOT_FOUND = "Seçilen kategori bulunamadı.";
    static final String HAS_STUDENTS = "Bu kursa kayıtlı kursiyerler olduğu için kurs silinemez.";

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserDirectoryService userDirectory;
    private final CourseMapper courseMapper;
    private final CourseOwnershipService ownership;
    private final EnrollmentCounts enrollmentCounts;
    private final ApplicationEventPublisher events;

    public CourseService(CourseRepository courseRepository, CategoryRepository categoryRepository,
            UserDirectoryService userDirectory, CourseMapper courseMapper, CourseOwnershipService ownership,
            EnrollmentCounts enrollmentCounts, ApplicationEventPublisher events) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.userDirectory = userDirectory;
        this.courseMapper = courseMapper;
        this.ownership = ownership;
        this.enrollmentCounts = enrollmentCounts;
        this.events = events;
    }

    @Transactional
    public CourseResponse create(Long instructorId, CreateCourseRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> BadRequestException.forField("categoryId", CATEGORY_NOT_FOUND));
        Course course = new Course(instructorId, category, request.title(), request.description(),
                request.level(), request.learningOutcomes());
        Course saved = courseRepository.saveAndFlush(course);
        UserSummary instructor = userDirectory.findSummaries(List.of(instructorId)).get(instructorId);
        if (instructor == null) {
            // Güvenlik filtresi kullanıcının var olduğunu doğruladı; buraya yalnız arada silinen kullanıcıda düşülür.
            throw new NotFoundException("Kullanıcı bulunamadı.");
        }
        return courseMapper.toResponse(saved, instructor);
    }

    /**
     * Kursu günceller (#23): kurs yoksa 404, eğitmeni değilse 403, kategori yoksa 400 ({@code errors.categoryId}),
     * kurs düzenlenemez durumdaysa 409 (Taslak ve Yayından kaldırıldı dışında; karar durum nesnesinde).
     */
    @Transactional
    public CourseResponse update(Long courseId, Long instructorId, UpdateCourseRequest request) {
        Course course = ownership.requireOwnedCourse(courseId, instructorId);
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> BadRequestException.forField("categoryId", CATEGORY_NOT_FOUND));
        course.update(category, request.title(), request.description(), request.level(), request.learningOutcomes());
        return toResponse(courseRepository.saveAndFlush(course));
    }

    /**
     * Kursu siler (#23): kurs yoksa 404, eğitmeni değilse 403, Taslak değilse 409, kursiyeri varsa 409.
     * Bölümler ve dersler kursla, soru bankası ve sınavlar veritabanında (ON DELETE CASCADE) silinir; kapak ve ders
     * dosyaları için {@link ContentFileDetached} yayınlanır, {@code content} commit'ten sonra diskten siler.
     */
    @Transactional
    public void delete(Long courseId, Long instructorId) {
        Course course = ownership.requireOwnedCourse(courseId, instructorId);
        course.assertDeletable();
        if (enrollmentCounts.countByCourse(courseId) > 0) {
            throw new ConflictException(HAS_STUDENTS);
        }
        List<Long> files = course.attachedFileIds();
        courseRepository.delete(course);
        files.forEach(fileId -> events.publishEvent(new ContentFileDetached(fileId)));
    }

    /**
     * Eğitmenin kendi kursları (#28): her durumda, en yeni önce; {@code status} verilirse yalnız o durum. Sayfalama
     * yoktur (sözleşme düz liste). Red ve kaldırma gerekçeleri kursta olduğu için yanıtta gelir.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> listByInstructor(Long instructorId, CourseStatus status) {
        List<Course> courses = courseRepository.findByInstructor(instructorId, status);
        if (courses.isEmpty()) {
            return List.of();
        }
        UserSummary instructor = userDirectory.findSummaries(List.of(instructorId)).get(instructorId);
        if (instructor == null) {
            throw new NotFoundException("Kullanıcı bulunamadı.");
        }
        return courses.stream().map(course -> courseMapper.toResponse(course, instructor)).toList();
    }

    private CourseResponse toResponse(Course course) {
        UserSummary instructor = userDirectory.findSummaries(List.of(course.getInstructorId()))
                .get(course.getInstructorId());
        if (instructor == null) {
            // Güvenlik filtresi kullanıcının var olduğunu doğruladı; buraya yalnız arada silinen kullanıcıda düşülür.
            throw new NotFoundException("Kullanıcı bulunamadı.");
        }
        return courseMapper.toResponse(course, instructor);
    }
}
