package com.miniudemy.course.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.dto.OrderedLesson;
import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.repository.SectionRepository;

/** Kursun bölüm/ders iskeleti; ilerleme modülü yüzdeyi buradaki ders sayılarından hesaplar. */
@Service
@Transactional(readOnly = true)
public class CourseStructureService {

    private final SectionRepository sectionRepository;

    public CourseStructureService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    /** Kursun dersleri, kurs içindeki sırasıyla (bölüm sırası, sonra ders sırası) tek sorguyla (#119). */
    public List<OrderedLesson> orderedLessons(Long courseId) {
        return sectionRepository.findAllWithLessonsByCourseId(courseId).stream()
                .flatMap(s -> s.getLessons().stream().map(l -> new OrderedLesson(l.getId(), s.getId(), l.getTitle())))
                .toList();
    }

    /** Kursun bölümleri ve ders id'leri tek sorguyla, bölüm sırasıyla. Dersi olmayan bölüm boş listeyle yer alır. */
    public List<SectionLessons> sectionLessons(Long courseId) {
        return sectionRepository.findAllWithLessonsByCourseId(courseId).stream()
                .map(s -> new SectionLessons(s.getId(), s.getLessons().stream().map(Lesson::getId).toList()))
                .toList();
    }
}
