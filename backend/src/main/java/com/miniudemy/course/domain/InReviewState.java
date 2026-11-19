package com.miniudemy.course.domain;

/** İncelemede: kurs kilitli; yönetici onaylarsa Yayımlandı, reddederse (gerekçeyle) Taslak. */
final class InReviewState implements CourseState {

    static final InReviewState INSTANCE = new InReviewState();

    private InReviewState() {
    }

    @Override
    public CourseStatus status() {
        return CourseStatus.IN_REVIEW;
    }

    @Override
    public CourseStatus approve() {
        return CourseStatus.PUBLISHED;
    }

    @Override
    public CourseStatus reject() {
        return CourseStatus.IN_REVIEW; // KIRMIZI-A3
    }
}
