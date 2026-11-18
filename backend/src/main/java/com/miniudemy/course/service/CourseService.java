package com.miniudemy.course.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Category;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CreateCourseRequest;
import com.miniudemy.course.mapper.CourseMapper;
import com.miniudemy.course.repository.CategoryRepository;
import com.miniudemy.course.repository.CourseRepository;

/**
 * Kurs işlemleri. Bu sürümde yalnız oluşturma (#22, FR-07); düzenleme/silme #23, durum geçişleri #27 ve #85'te.
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

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserDirectoryService userDirectory;
    private final CourseMapper courseMapper;

    public CourseService(CourseRepository courseRepository, CategoryRepository categoryRepository,
            UserDirectoryService userDirectory, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.userDirectory = userDirectory;
        this.courseMapper = courseMapper;
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
}
