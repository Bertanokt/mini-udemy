package com.miniudemy.course.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.repository.LessonRepository;

/** Başka modüllerin bir ders hakkında ihtiyaç duyduğu bilgiler (ör. progress, #41). Entity dışarı verilmez, id verilir. */
@Service
public class LessonLookupService {

    private final LessonRepository lessonRepository;

    public LessonLookupService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    /** Dersin bağlı olduğu kursun id'si; ders yoksa 404. */
    @Transactional(readOnly = true)
    public Long courseIdOfLesson(Long lessonId) {
        return lessonRepository.findCourseIdByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Ders bulunamadı."));
    }
}
