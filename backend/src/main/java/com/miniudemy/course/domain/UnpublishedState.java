package com.miniudemy.course.domain;

/** Yayından kaldırıldı: katalogda görünmez; eğitmen düzenleyip yeniden onaya gönderebilir. Silinemez. */
final class UnpublishedState implements CourseState {

    static final UnpublishedState INSTANCE = new UnpublishedState();

    private UnpublishedState() {
    }

    @Override
    public CourseStatus status() {
        return CourseStatus.UNPUBLISHED;
    }

    @Override
    public void assertEditable() {
    }

    @Override
    public void assertLessonAddable() {
    }

    @Override
    public CourseStatus submit() {
        return CourseStatus.IN_REVIEW;
    }
}
