package com.miniudemy.course.domain;

/** Yayımlandı: katalogda görünür, kilitli (yalnız yeni ders eklenebilir); yayından kaldırılabilir. */
final class PublishedState implements CourseState {

    static final PublishedState INSTANCE = new PublishedState();

    private PublishedState() {
    }

    @Override
    public CourseStatus status() {
        return CourseStatus.PUBLISHED;
    }

    @Override
    public void assertLessonAddable() {
    }

    @Override
    public CourseStatus unpublish() {
        return CourseStatus.DRAFT; // KIRMIZI-B2
    }

    @Override
    public boolean isPublished() {
        return true;
    }
}
