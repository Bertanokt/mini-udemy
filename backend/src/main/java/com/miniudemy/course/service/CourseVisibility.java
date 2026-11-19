package com.miniudemy.course.service;

import org.springframework.stereotype.Component;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Course;

/**
 * Kurs detayı ve bölüm listesini kimin görebileceği (#26, #24, #123). Yayımlanmış kurs herkese açıktır. Yayımlanmamış
 * kursu yalnız sahibi, yönetici ve kursa kayıtlı kursiyer görür; diğerlerine kursun varlığı açığa çıkmadan 404.
 *
 * Kayıtlı kursiyer kuralı yayından kaldırılan kurs içindir (FR-38): önceden kaydolanlar derslerine devam eder, içerik
 * erişimi de aynı kuraldır (ContentDeliveryService). Kayıt bilgisi {@code enrollment}'tadır; {@code course} ona
 * bağımlı olamayacağı için {@link CourseRoster} arayüzünden sorulur (bağımlılığın tersine çevrilmesi).
 */
@Component
public class CourseVisibility {

    static final String COURSE_NOT_FOUND = "Kurs bulunamadı.";

    private final CourseRoster roster;

    public CourseVisibility(CourseRoster roster) {
        this.roster = roster;
    }

    /** Görüntüleyen kursu göremiyorsa 404. Kayıt sorgusu yalnız diğer kurallar izin vermediğinde yapılır. */
    public void assertVisible(Course course, Long viewerId, boolean viewerIsAdmin) {
        if (course.isPublished() || viewerIsAdmin) {
            return;
        }
        if (viewerId != null
                && (viewerId.equals(course.getInstructorId()) || roster.isEnrolled(course.getId(), viewerId))) {
            return;
        }
        throw new NotFoundException(COURSE_NOT_FOUND);
    }
}
