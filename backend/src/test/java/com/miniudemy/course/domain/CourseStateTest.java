package com.miniudemy.course.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.course.CourseFixtures;

/**
 * State deseni (#85): kursun 4 durumu × 6 işlemin 24 birleşiminin tamamı. Geçiş tablosu docs/architecture.md ›
 * Kurs durumları ile aynıdır (FR-09, FR-10, FR-38). İzinsiz işlem 409 ({@link CourseStateException}) fırlatır ve
 * kursu değiştirmez.
 */
class CourseStateTest {

    /** Testteki işlem adı → kursa uygulanan işlem. */
    private static Consumer<Course> operation(String name) {
        return switch (name) {
            case "EDIT" -> Course::assertEditable;
            case "DELETE" -> Course::assertDeletable;
            case "SUBMIT" -> Course::submit;
            case "APPROVE" -> Course::approve;
            case "REJECT" -> c -> c.reject("Eksik içerik");
            case "UNPUBLISH" -> c -> c.unpublish("Şikâyet");
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static Course courseIn(CourseStatus status) {
        return CourseFixtures.course(5L, 7L, "Kurs", status);
    }

    /** Sonuç: izinli geçişte hedef durum, izinli kontrolde (düzenle/sil) "OK", izinsizde "409". */
    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
        "DRAFT,       EDIT,      OK",
        "DRAFT,       DELETE,    OK",
        "DRAFT,       SUBMIT,    IN_REVIEW",
        "DRAFT,       APPROVE,   409",
        "DRAFT,       REJECT,    409",
        "DRAFT,       UNPUBLISH, 409",
        "IN_REVIEW,   EDIT,      409",
        "IN_REVIEW,   DELETE,    409",
        "IN_REVIEW,   SUBMIT,    409",
        "IN_REVIEW,   APPROVE,   PUBLISHED",
        "IN_REVIEW,   REJECT,    DRAFT",
        "IN_REVIEW,   UNPUBLISH, 409",
        "PUBLISHED,   EDIT,      409",
        "PUBLISHED,   DELETE,    409",
        "PUBLISHED,   SUBMIT,    409",
        "PUBLISHED,   APPROVE,   409",
        "PUBLISHED,   REJECT,    409",
        "PUBLISHED,   UNPUBLISH, UNPUBLISHED",
        "UNPUBLISHED, EDIT,      OK",
        "UNPUBLISHED, DELETE,    409",
        "UNPUBLISHED, SUBMIT,    IN_REVIEW",
        "UNPUBLISHED, APPROVE,   409",
        "UNPUBLISHED, REJECT,    409",
        "UNPUBLISHED, UNPUBLISH, 409",
    })
    void everyOperationInEveryStatus_followsTheTransitionTable(CourseStatus from, String op, String expected) {
        Course course = courseIn(from);

        if (expected.equals("409")) {
            assertThatThrownBy(() -> operation(op).accept(course))
                    .isInstanceOf(CourseStateException.class)
                    .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(course.getStatus()).as("izinsiz işlem durumu değiştirmez").isEqualTo(from);
            assertThat(course.getRejectionReason()).isNull();
            assertThat(course.getUnpublishReason()).isNull();
        } else if (expected.equals("OK")) {
            assertThatCode(() -> operation(op).accept(course)).doesNotThrowAnyException();
            assertThat(course.getStatus()).as("kontrol durumu değiştirmez").isEqualTo(from);
        } else {
            operation(op).accept(course);
            assertThat(course.getStatus()).isEqualTo(CourseStatus.valueOf(expected));
        }
    }

    @Test
    void editInLockedStatus_keepsTheExistingUserMessage() {
        assertThatThrownBy(() -> courseIn(CourseStatus.IN_REVIEW).assertEditable())
                .hasMessage("Kurs bu durumdayken düzenlenemez; yalnız taslak ve yayından kaldırılmış kurslar düzenlenebilir.");
    }

    @Test
    void reject_storesTheReason_andSubmitAfterwardsClearsIt() {
        Course course = courseIn(CourseStatus.IN_REVIEW);

        course.reject("Ders içerikleri eksik.");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.getRejectionReason()).isEqualTo("Ders içerikleri eksik.");

        course.submit();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.IN_REVIEW);
        assertThat(course.getRejectionReason()).as("yeniden onaya gönderince eski red gerekçesi temizlenir").isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL", "''", "'   '"}, nullValues = "NULL")
    void reject_withoutReason_isRefused_andCourseStaysInReview(String reason) {
        Course course = courseIn(CourseStatus.IN_REVIEW);

        assertThatThrownBy(() -> course.reject(reason)).isInstanceOf(IllegalArgumentException.class);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.IN_REVIEW);
        assertThat(course.getRejectionReason()).isNull();
    }

    @Test
    void unpublish_storesTheOptionalReason() {
        Course byAdmin = courseIn(CourseStatus.PUBLISHED);
        byAdmin.unpublish("Telif şikâyeti");
        assertThat(byAdmin.getUnpublishReason()).isEqualTo("Telif şikâyeti");

        Course byInstructor = courseIn(CourseStatus.PUBLISHED);
        byInstructor.unpublish(null);
        assertThat(byInstructor.getStatus()).isEqualTo(CourseStatus.UNPUBLISHED);
        assertThat(byInstructor.getUnpublishReason()).isNull();
    }

    @ParameterizedTest
    @EnumSource(CourseStatus.class)
    void onlyPublishedCourseIsPublished(CourseStatus status) {
        assertThat(courseIn(status).isPublished()).isEqualTo(status == CourseStatus.PUBLISHED);
    }

    @ParameterizedTest
    @EnumSource(CourseStatus.class)
    void everyStoredStatusMapsToTheStateObjectOfThatStatus(CourseStatus status) {
        assertThat(status.state().status()).isEqualTo(status);
    }
}
