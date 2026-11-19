package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.exception.UnauthorizedException;
import com.miniudemy.common.security.AccountLookup;
import com.miniudemy.common.security.ContentTokens;
import com.miniudemy.common.security.IssuedContentToken;
import com.miniudemy.common.security.SecurityMessages;
import com.miniudemy.content.domain.DeliveryMode;
import com.miniudemy.content.domain.LessonOpened;
import com.miniudemy.content.dto.ContentDownload;
import com.miniudemy.content.dto.StoredFileInfo;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.dto.LessonContentRef;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.course.service.LessonLookupService;
import com.miniudemy.enrollment.service.EnrollmentService;

/**
 * Yetki kontrollü içerik servisi (#113): kursa kayıtlı kursiyer, kursun eğitmeni ve yönetici erişir; diğerleri 403.
 * Kayıtsız kursiyerin 403 alması proje dokümanı Bölüm 6'nın "kayıtsız erişim" test durumudur; silinmez,
 * {@code @Disabled} yapılmaz. Sunuluş türün ürününden (Factory Method, #110).
 */
class ContentDeliveryServiceTest {

    private static final long STUDENT = 42L;
    private static final long INSTRUCTOR = 7L;
    private static final long OTHER_INSTRUCTOR = 8L;
    private static final long ADMIN = 1L;

    private final LessonLookupService lessons = mock(LessonLookupService.class);
    private final CourseLookupService courses = mock(CourseLookupService.class);
    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final StorageService storage = mock(StorageService.class);
    private static final Instant NOW = Instant.parse("2026-11-18T15:00:00Z");
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ContentTokens contentTokens = mock(ContentTokens.class);
    private final AccountLookup accounts = mock(AccountLookup.class);
    private final ContentDeliveryService service = new ContentDeliveryService(lessons, courses, enrollments, storage,
            new LessonContentFactories(List.of(new VideoContentFactory(), new PdfContentFactory(),
                    new DocumentContentFactory())), events, Clock.fixed(NOW, ZoneOffset.UTC), contentTokens, accounts);

    private final Resource bytes = new ByteArrayResource("0123456789".getBytes());

    @BeforeEach
    void setUp() {
        given(lessons.contentRefOf(11L)).willReturn(new LessonContentRef(11L, 3L, INSTRUCTOR, LessonContentType.VIDEO, 900L));
        given(enrollments.isEnrolled(STUDENT, 3L)).willReturn(true);
        given(storage.get(900L)).willReturn(new StoredFileInfo(900L, "Kurulum.mp4", "video/mp4", 10));
        given(storage.open(900L)).willReturn(bytes);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"kayıtlı kursiyer, 42, false", "kursun eğitmeni, 7, false", "yönetici, 1, true"})
    void openLessonContent_allowedViewer_getsTheFileWithItsTypeAndDelivery(String who, long viewer, boolean admin) {
        ContentDownload download = service.openLessonContent(11L, viewer, admin);

        assertThat(download.resource()).isSameAs(bytes);
        assertThat(download.mimeType()).isEqualTo("video/mp4");
        assertThat(download.fileName()).isEqualTo("Kurulum.mp4");
        assertThat(download.sizeBytes()).isEqualTo(10);
        assertThat(download.deliveryMode()).isEqualTo(DeliveryMode.INLINE);
        assertThat(download.rangeRequests()).isTrue();
    }

    /** Proje dokümanı Bölüm 6: kayıtsız kursiyer ders içeriğine erişemez. */
    @Test
    void openLessonContent_unenrolledStudent_returns403_andOpensNothing() {
        assertThatThrownBy(() -> service.openLessonContent(11L, 43L, false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Bu ders içeriğine erişim yetkiniz yok.");
        verify(storage, never()).open(anyLong());
    }

    @Test
    void openLessonContent_instructorOfAnotherCourse_returns403() {
        assertThatThrownBy(() -> service.openLessonContent(11L, OTHER_INSTRUCTOR, false))
                .isInstanceOf(ForbiddenException.class);
        verify(storage, never()).open(anyLong());
    }

    @Test
    void openLessonContent_lessonWithoutContent_returns404_butOnlyToAllowedViewers() {
        given(lessons.contentRefOf(12L)).willReturn(new LessonContentRef(12L, 3L, INSTRUCTOR, LessonContentType.PDF, null));

        assertThatThrownBy(() -> service.openLessonContent(12L, STUDENT, false))
                .isInstanceOf(NotFoundException.class).hasMessage("Bu derse henüz içerik yüklenmemiş.");
        assertThatThrownBy(() -> service.openLessonContent(12L, 43L, false))
                .as("yetkisiz kullanıcıya içeriğin olup olmadığı açığa çıkmaz").isInstanceOf(ForbiddenException.class);
    }

    @Test
    void openLessonContent_unknownLesson_returns404() {
        given(lessons.contentRefOf(99L)).willThrow(new NotFoundException("Ders bulunamadı."));

        assertThatThrownBy(() -> service.openLessonContent(99L, ADMIN, true)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void openLessonContent_documentLesson_isDownloadedWithoutRange_evenForAPdfFile() {
        given(lessons.contentRefOf(13L)).willReturn(new LessonContentRef(13L, 3L, INSTRUCTOR, LessonContentType.DOCUMENT, 901L));
        given(storage.get(901L)).willReturn(new StoredFileInfo(901L, "Notlar.pdf", "application/pdf", 10));
        given(storage.open(901L)).willReturn(bytes);

        ContentDownload download = service.openLessonContent(13L, STUDENT, false);

        assertThat(download.mimeType()).isEqualTo("application/pdf");
        assertThat(download.deliveryMode()).isEqualTo(DeliveryMode.ATTACHMENT);
        assertThat(download.rangeRequests()).isFalse();
    }

    @Test
    void openCourseCover_withCover_isInlineWithoutRange_forAnyone() {
        given(courses.coverFileIdOf(3L)).willReturn(Optional.of(950L));
        given(storage.get(950L)).willReturn(new StoredFileInfo(950L, "kapak.png", "image/png", 10));
        given(storage.open(950L)).willReturn(bytes);

        ContentDownload download = service.openCourseCover(3L);

        assertThat(download.mimeType()).isEqualTo("image/png");
        assertThat(download.deliveryMode()).isEqualTo(DeliveryMode.INLINE);
        assertThat(download.rangeRequests()).isFalse();
    }

    @Test
    void openCourseCover_withoutCover_returns404() {
        given(courses.coverFileIdOf(4L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.openCourseCover(4L))
                .isInstanceOf(NotFoundException.class).hasMessage("Kursun kapak görseli yok.");
        verify(storage, never()).open(any());
    }

    // --- son izlenen ders olayı (#119) ---

    @Test
    void openLessonContent_enrolledStudent_publishesLessonOpened() {
        service.openLessonContent(11L, STUDENT, false);

        verify(events).publishEvent(new LessonOpened(STUDENT, 3L, 11L, NOW));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"kursun eğitmeni, 7, false", "yönetici, 1, true"})
    void openLessonContent_instructorOrAdmin_publishesNothing(String who, long viewer, boolean admin) {
        service.openLessonContent(11L, viewer, admin);

        verifyNoInteractions(events);
    }

    @Test
    void openLessonContent_refusedOrWithoutContent_publishesNothing() {
        given(lessons.contentRefOf(12L)).willReturn(new LessonContentRef(12L, 3L, INSTRUCTOR, LessonContentType.PDF, null));

        assertThatThrownBy(() -> service.openLessonContent(11L, 43L, false)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.openLessonContent(12L, STUDENT, false)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(events);
    }

    // --- içerik token'ı (#45, ADR-012) ---

    @Test
    void issueContentToken_allowedViewer_getsATokenForThatLesson() {
        IssuedContentToken issued = new IssuedContentToken("t", NOW.plusSeconds(300));
        given(contentTokens.issue(STUDENT, 11L)).willReturn(issued);

        assertThat(service.issueContentToken(11L, STUDENT, false)).isSameAs(issued);
        verifyNoInteractions(events);
    }

    @Test
    void issueContentToken_unenrolledStudent_returns403_andIssuesNothing() {
        assertThatThrownBy(() -> service.issueContentToken(11L, 43L, false)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(contentTokens);
    }

    @Test
    void issueContentToken_lessonWithoutContent_returns404() {
        given(lessons.contentRefOf(12L)).willReturn(new LessonContentRef(12L, 3L, INSTRUCTOR, LessonContentType.PDF, null));

        assertThatThrownBy(() -> service.issueContentToken(12L, STUDENT, false)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(contentTokens);
    }

    @Test
    void openWithToken_validToken_opensForTheTokenUser_andRecordsTheLessonOpened() {
        given(contentTokens.verify("t", 11L)).willReturn(Optional.of(STUDENT));
        given(accounts.findById(STUDENT)).willReturn(Optional.of(new AccountLookup.AccountSnapshot(true, "STUDENT")));

        ContentDownload download = service.openLessonContentWithToken(11L, "t");

        assertThat(download.resource()).isSameAs(bytes);
        verify(events).publishEvent(new LessonOpened(STUDENT, 3L, 11L, NOW));
    }

    @Test
    void openWithToken_adminRoleComesFromTheDatabase() {
        given(contentTokens.verify("t", 11L)).willReturn(Optional.of(ADMIN));
        given(accounts.findById(ADMIN)).willReturn(Optional.of(new AccountLookup.AccountSnapshot(true, "ADMIN")));

        assertThat(service.openLessonContentWithToken(11L, "t").resource()).isSameAs(bytes);
        verifyNoInteractions(events);
    }

    @Test
    void openWithToken_tokenUserNoLongerEnrolled_returns403() {
        given(contentTokens.verify("t", 11L)).willReturn(Optional.of(43L));
        given(accounts.findById(43L)).willReturn(Optional.of(new AccountLookup.AccountSnapshot(true, "STUDENT")));

        assertThatThrownBy(() -> service.openLessonContentWithToken(11L, "t")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void openWithToken_invalidOrExpiredToken_returns401_andOpensNothing() {
        given(contentTokens.verify("x", 11L)).willReturn(Optional.empty());
        // Hesap aktif ve kayıtlı: 401'in tek sebebi token olsun.
        given(accounts.findById(STUDENT)).willReturn(Optional.of(new AccountLookup.AccountSnapshot(true, "STUDENT")));

        assertThatThrownBy(() -> service.openLessonContentWithToken(11L, "x"))
                .isInstanceOf(UnauthorizedException.class).hasMessage(SecurityMessages.UNAUTHORIZED);
        verify(storage, never()).open(anyLong());
    }

    @Test
    void openWithToken_deletedUser_returns401_andDisabledUser_returns403() {
        given(contentTokens.verify("gone", 11L)).willReturn(Optional.of(77L));
        given(accounts.findById(77L)).willReturn(Optional.empty());
        given(contentTokens.verify("off", 11L)).willReturn(Optional.of(STUDENT));
        given(accounts.findById(STUDENT)).willReturn(Optional.of(new AccountLookup.AccountSnapshot(false, "STUDENT")));

        assertThatThrownBy(() -> service.openLessonContentWithToken(11L, "gone")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.openLessonContentWithToken(11L, "off"))
                .isInstanceOf(ForbiddenException.class).hasMessage(SecurityMessages.ACCOUNT_DISABLED);
        verify(storage, never()).open(anyLong());
    }
}
