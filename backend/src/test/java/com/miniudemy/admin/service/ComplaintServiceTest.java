package com.miniudemy.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.admin.domain.Complaint;
import com.miniudemy.admin.domain.ComplaintStatus;
import com.miniudemy.admin.dto.ComplaintPage;
import com.miniudemy.admin.dto.ComplaintResponse;
import com.miniudemy.admin.dto.CreateComplaintRequest;
import com.miniudemy.admin.mapper.ComplaintMapperImpl;
import com.miniudemy.admin.repository.ComplaintRepository;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.review.service.ReviewService;

/** Şikâyet (#144): kurs ya da yorum şikâyeti, hedef kuralı, varlık denetimi, yönetici listesi ve durum değişikliği. */
class ComplaintServiceTest {

    private final ComplaintRepository repository = mock(ComplaintRepository.class);
    private final CourseLookupService courseLookup = mock(CourseLookupService.class);
    private final ReviewService reviewService = mock(ReviewService.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final ComplaintService service = new ComplaintService(repository, courseLookup, reviewService,
            userDirectory, new ComplaintMapperImpl());

    private static Complaint complaint(long id, long reporterId, Long courseId, Long reviewId) {
        Complaint c = new Complaint(reporterId, courseId, reviewId, "Kurs içeriği yanıltıcı.");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private void saveAssignsId99() {
        given(repository.save(any(Complaint.class))).willAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 99L);
            return c;
        });
    }

    private void reporter(long id, String name) {
        given(userDirectory.findSummaries(any())).willReturn(Map.of(id, new UserSummary(id, name)));
    }

    // ---- create

    @Test
    void create_aboutPublishedCourse_savesOpenComplaintWithReporter() {
        saveAssignsId99();
        reporter(42L, "Ayşe Kaya");

        ComplaintResponse response = service.create(42L,
                new CreateComplaintRequest(5L, null, "  Kurs içeriği yanıltıcı.  "));

        verify(courseLookup).assertPublished(5L);
        verifyNoInteractions(reviewService);
        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.courseId()).isEqualTo(5L);
        assertThat(response.reviewId()).isNull();
        assertThat(response.reason()).isEqualTo("Kurs içeriği yanıltıcı.");
        assertThat(response.status()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(response.reporter()).isEqualTo(new UserSummary(42L, "Ayşe Kaya"));
        assertThat(response.resolvedAt()).isNull();
    }

    @Test
    void create_aboutVisibleReview_savesOpenComplaint() {
        saveAssignsId99();
        reporter(42L, "Ayşe Kaya");

        ComplaintResponse response = service.create(42L, new CreateComplaintRequest(null, 9L, "Hakaret içeriyor."));

        verify(reviewService).assertVisible(9L);
        verifyNoInteractions(courseLookup);
        assertThat(response.reviewId()).isEqualTo(9L);
        assertThat(response.courseId()).isNull();
        assertThat(response.status()).isEqualTo(ComplaintStatus.OPEN);
    }

    @Test
    void create_withBothTargetsOrNone_returns400_andSavesNothing() {
        for (CreateComplaintRequest request : List.of(
                new CreateComplaintRequest(5L, 9L, "Kurs içeriği yanıltıcı."),
                new CreateComplaintRequest(null, null, "Kurs içeriği yanıltıcı."))) {
            assertThatThrownBy(() -> service.create(42L, request)).isInstanceOf(BadRequestException.class);
        }
        verify(repository, never()).save(any());
        verifyNoInteractions(courseLookup, reviewService);
    }

    @Test
    void create_aboutMissingOrUnpublishedCourse_returns404_andSavesNothing() {
        willThrow(new NotFoundException("Kurs bulunamadı.")).given(courseLookup).assertPublished(5L);

        assertThatThrownBy(() -> service.create(42L, new CreateComplaintRequest(5L, null, "Kurs içeriği yanıltıcı.")))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_aboutMissingOrHiddenReview_returns404_andSavesNothing() {
        willThrow(new NotFoundException("Değerlendirme bulunamadı.")).given(reviewService).assertVisible(9L);

        assertThatThrownBy(() -> service.create(42L, new CreateComplaintRequest(null, 9L, "Hakaret içeriyor.")))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    // ---- list

    @Test
    void list_withoutStatus_returnsAllNewestFirst_withReporterNames() {
        given(repository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(
                List.of(complaint(2L, 42L, null, 9L), complaint(1L, 43L, 5L, null)), PageRequest.of(0, 20), 2));
        given(userDirectory.findSummaries(any())).willReturn(Map.of(42L, new UserSummary(42L, "Ayşe Kaya")));

        ComplaintPage page = service.list(null, 0, 20);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(ComplaintResponse::id).containsExactly(2L, 1L);
        assertThat(page.content().get(0).reporter().fullName()).isEqualTo("Ayşe Kaya");
        assertThat(page.content().get(1).reporter()).isEqualTo(new UserSummary(43L, "Kullanıcı"));
        verify(repository).findAll(PageRequest.of(0, 20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Test
    void list_withStatus_returnsOnlyThatStatus() {
        given(repository.findByStatus(any(), any())).willReturn(new PageImpl<>(List.of()));
        given(userDirectory.findSummaries(any())).willReturn(Map.of());

        assertThat(service.list(ComplaintStatus.OPEN, 1, 10).content()).isEmpty();
        verify(repository).findByStatus(ComplaintStatus.OPEN, PageRequest.of(1, 10,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        verify(repository, never()).findAll(any(Pageable.class));
    }

    // ---- setStatus

    @Test
    void setStatus_reviewed_resolvesComplaintWithAdminAndTime() {
        Complaint open = complaint(7L, 42L, 5L, null);
        given(repository.findById(7L)).willReturn(Optional.of(open));
        reporter(42L, "Ayşe Kaya");

        ComplaintResponse response = service.setStatus(7L, ComplaintStatus.REVIEWED, 1L);

        assertThat(response.status()).isEqualTo(ComplaintStatus.REVIEWED);
        assertThat(response.resolvedAt()).isNotNull();
        assertThat(open.getResolvedById()).isEqualTo(1L);
    }

    @Test
    void setStatus_missingComplaint_returns404() {
        given(repository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStatus(7L, ComplaintStatus.REJECTED, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ComplaintService.NOT_FOUND);
    }

    @Test
    void setStatus_backToOpen_returns400_andComplaintStaysUnresolved() {
        Complaint open = complaint(7L, 42L, 5L, null);
        given(repository.findById(7L)).willReturn(Optional.of(open));

        assertThatThrownBy(() -> service.setStatus(7L, ComplaintStatus.OPEN, 1L))
                .isInstanceOf(BadRequestException.class);
        assertThat(open.getResolvedById()).isNull();
    }
}
