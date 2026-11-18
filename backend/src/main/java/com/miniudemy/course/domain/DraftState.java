package com.miniudemy.course.domain;

/** Taslak: eğitmen düzenler ve silebilir; onaya gönderince İncelemede. */
final class DraftState implements CourseState {

    static final DraftState INSTANCE = new DraftState();

    private DraftState() {
    }

    @Override
    public CourseStatus status() {
        return CourseStatus.DRAFT;
    }

    @Override
    public void assertEditable() {
    }

    @Override
    public void assertDeletable() {
    }

    @Override
    public CourseStatus submit() {
        return CourseStatus.IN_REVIEW;
    }
}
