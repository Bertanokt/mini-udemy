package com.miniudemy.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.miniudemy.common.exception.BadRequestException;

/** Şikâyet kuralları (#144): tek hedef, açık doğar, yalnız İncelendi / Reddedildi olarak sonuçlanır. */
class ComplaintTest {

    @Test
    void newComplaint_aboutACourseOrAReview_isOpenAndUnresolved() {
        Complaint aboutCourse = new Complaint(42L, 5L, null, "Kurs içeriği yanıltıcı.");
        Complaint aboutReview = new Complaint(42L, null, 9L, "Hakaret içeriyor.");

        assertThat(aboutCourse.isAboutCourse()).isTrue();
        assertThat(aboutReview.isAboutCourse()).isFalse();
        assertThat(aboutCourse.getStatus()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(aboutCourse.getCreatedAt()).isNotNull();
        assertThat(aboutCourse.getResolvedAt()).isNull();
        assertThat(aboutCourse.getResolvedById()).isNull();
    }

    @Test
    void newComplaint_withBothTargetsOrNone_isRejectedWith400() {
        assertThatThrownBy(() -> new Complaint(42L, 5L, 9L, "Kurs içeriği yanıltıcı."))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(Complaint.SINGLE_TARGET);
        assertThatThrownBy(() -> new Complaint(42L, null, null, "Kurs içeriği yanıltıcı."))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(Complaint.SINGLE_TARGET);
    }

    @Test
    void resolve_recordsResultAdminAndTime_andCanBeChangedLater() {
        Complaint complaint = new Complaint(42L, 5L, null, "Kurs içeriği yanıltıcı.");

        complaint.resolve(ComplaintStatus.REVIEWED, 1L);
        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.REVIEWED);
        assertThat(complaint.getResolvedById()).isEqualTo(1L);
        assertThat(complaint.getResolvedAt()).isNotNull();

        complaint.resolve(ComplaintStatus.REJECTED, 2L);
        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.REJECTED);
        assertThat(complaint.getResolvedById()).isEqualTo(2L);
    }

    @Test
    void resolve_backToOpen_isRejectedWithStatusFieldError_andChangesNothing() {
        Complaint complaint = new Complaint(42L, 5L, null, "Kurs içeriği yanıltıcı.");

        assertThatThrownBy(() -> complaint.resolve(ComplaintStatus.OPEN, 1L))
                .isInstanceOfSatisfying(BadRequestException.class,
                        e -> assertThat(e.getErrors()).containsEntry("status", Complaint.RESOLVE_ONLY));
        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(complaint.getResolvedById()).isNull();
    }
}
