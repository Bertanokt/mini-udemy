package com.miniudemy.course.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.SectionRequest;
import com.miniudemy.course.dto.SectionResponse;
import com.miniudemy.course.mapper.CourseMapper;
import com.miniudemy.course.repository.CourseRepository;
import com.miniudemy.course.repository.SectionRepository;

/**
 * Bölüm işlemleri (#24, FR-08). Kurallar docs/api/openapi.yaml › Sections'tandır.
 *
 * <ul>
 *   <li>Liste herkese açık; yayımlanmamış kursun bölümleri yalnız sahibine ve yöneticiye görünür, diğerlerine
 *       kursun varlığı açığa çıkmadan 404 (kurs detayıyla aynı kural).</li>
 *   <li>Değiştiren her işlem: kurs ya da bölüm yoksa 404, kullanıcı kursun eğitmeni değilse 403, kurs düzenlenemez
 *       durumdaysa 409 (sahiplik {@link CourseOwnershipService}'te, düzenlenebilirlik kursun durum nesnesinde).</li>
 *   <li>Yeni bölüm sona eklenir; silmede kalanlar, taşımada aradakiler boşluksuz yeniden sıralanır (sıra 0'dan).</li>
 * </ul>
 */
@Service
public class SectionService {

    static final String COURSE_NOT_FOUND = "Kurs bulunamadı.";
    static final String SECTION_NOT_FOUND = "Bölüm bulunamadı.";

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final CourseOwnershipService ownership;
    private final CourseMapper mapper;

    public SectionService(SectionRepository sectionRepository, CourseRepository courseRepository,
            CourseOwnershipService ownership, CourseMapper mapper) {
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.ownership = ownership;
        this.mapper = mapper;
    }

    /** Kursun bölümleri, dersleriyle birlikte `position` sırasıyla (tek sorgu). */
    @Transactional(readOnly = true)
    public List<SectionResponse> listSections(Long courseId, Long viewerId, boolean viewerIsAdmin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(COURSE_NOT_FOUND));
        boolean owner = viewerId != null && viewerId.equals(course.getInstructorId());
        if (!course.isPublished() && !owner && !viewerIsAdmin) {
            throw new NotFoundException(COURSE_NOT_FOUND);
        }
        return sectionRepository.findAllWithLessonsByCourseId(courseId).stream()
                .map(mapper::toSectionResponse)
                .toList();
    }

    /** Bölümü kursun sonuna ekler. */
    @Transactional
    public SectionResponse createSection(Long courseId, Long userId, SectionRequest request) {
        Course course = courseRepository.findById(courseId).orElseThrow(); // KIRMIZI-1
        course.assertEditable();
        Section section = course.appendSection(request.title());
        sectionRepository.saveAndFlush(section);
        return mapper.toSectionResponse(section);
    }

    /** Bölüm başlığını değiştirir. */
    @Transactional
    public SectionResponse updateSection(Long sectionId, Long userId, SectionRequest request) {
        Section section = requireEditableSection(sectionId, userId);
        section.rename(request.title());
        return mapper.toSectionResponse(section);
    }

    /** Bölümü dersleriyle birlikte siler; kalan bölümler yeniden sıralanır. */
    @Transactional
    public void deleteSection(Long sectionId, Long userId) {
        Section section = requireEditableSection(sectionId, userId);
        section.getCourse().removeSection(section);
    }

    /** Bölümü verilen sıraya taşır; kursun yeni sıradaki bölümlerini döner. Sıra aralık dışıysa 400. */
    @Transactional
    public List<SectionResponse> moveSection(Long sectionId, Long userId, int position) {
        Section section = requireEditableSection(sectionId, userId);
        Course course = section.getCourse();
        int count = course.getSections().size();
        if (position >= count) {
            throw BadRequestException.forField("position", "Sıra 0 ile " + (count - 1) + " arasında olmalıdır.");
        }
        // KIRMIZI-3 course.moveSection(section, position);
        return course.getSections().stream().map(mapper::toSectionResponse).toList();
    }

    /** Bölüm yoksa 404; kursun eğitmeni değilse 403; kurs düzenlenemez durumdaysa 409. */
    private Section requireEditableSection(Long sectionId, Long userId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException(SECTION_NOT_FOUND));
        ownership.requireOwnedCourse(section.getCourse().getId(), userId).assertEditable();
        return section;
    }
}
