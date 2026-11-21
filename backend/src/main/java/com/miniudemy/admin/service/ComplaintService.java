package com.miniudemy.admin.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.admin.domain.Complaint;
import com.miniudemy.admin.domain.ComplaintStatus;
import com.miniudemy.admin.dto.ComplaintPage;
import com.miniudemy.admin.dto.ComplaintResponse;
import com.miniudemy.admin.dto.CreateComplaintRequest;
import com.miniudemy.admin.mapper.ComplaintMapper;
import com.miniudemy.admin.repository.ComplaintRepository;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.review.service.ReviewService;

/**
 * Şikâyet oluşturma ve yönetici incelemesi (#144, FR-41, UC-09 ve UC-30). Kurs ve yorumun varlığı kendi modüllerinin
 * service arayüzünden sorulur. İnce kapsam: yorumu gizlemek ya da kursu yayından kaldırmak bu servisin işi değildir;
 * yönetici bunları kendi uçlarından yapar (#142, #123). Şikâyet sahibine bildirim gitmez.
 */
@Service
public class ComplaintService {

    static final String NOT_FOUND = "Şikâyet bulunamadı.";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ComplaintRepository repository;
    private final CourseLookupService courseLookup;
    private final ReviewService reviewService;
    private final UserDirectoryService userDirectory;
    private final ComplaintMapper mapper;

    public ComplaintService(ComplaintRepository repository, CourseLookupService courseLookup,
            ReviewService reviewService, UserDirectoryService userDirectory, ComplaintMapper mapper) {
        this.repository = repository;
        this.courseLookup = courseLookup;
        this.reviewService = reviewService;
        this.userDirectory = userDirectory;
        this.mapper = mapper;
    }

    /**
     * Açık şikâyet oluşturur. Hedef tam biri değilse 400; kurs yayımlanmamışsa ya da yoksa, yorum gizliyse ya da
     * yoksa 404 (kullanıcının göremediği şey şikâyet edilemez).
     */
    @Transactional
    public ComplaintResponse create(Long reporterId, CreateComplaintRequest request) {
        Complaint complaint = new Complaint(reporterId, request.courseId(), request.reviewId(),
                request.reason().strip());
        if (complaint.isAboutCourse()) {
            courseLookup.assertPublished(complaint.getCourseId());
        } else {
            reviewService.assertVisible(complaint.getReviewId());
        }
        return toResponse(repository.save(complaint));
    }

    /** Yönetici listesi, en yeni önce; {@code status} verilirse yalnız o durumdakiler. */
    @Transactional(readOnly = true)
    public ComplaintPage list(ComplaintStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, NEWEST_FIRST);
        Page<Complaint> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        Map<Long, UserSummary> reporters = userDirectory.findSummaries(
                result.getContent().stream().map(Complaint::getReporterId).distinct().toList());
        List<ComplaintResponse> content = result.getContent().stream()
                .map(c -> mapper.toResponse(c, reporterOf(c, reporters)))
                .toList();
        return new ComplaintPage(content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    /** Durumu REVIEWED ya da REJECTED yapar; kural {@link Complaint#resolve}'da. Şikâyet yoksa 404. */
    @Transactional
    public ComplaintResponse setStatus(Long complaintId, ComplaintStatus status, Long adminId) {
        Complaint complaint = repository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        complaint.resolve(status, adminId);
        return toResponse(complaint);
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return mapper.toResponse(complaint,
                reporterOf(complaint, userDirectory.findSummaries(List.of(complaint.getReporterId()))));
    }

    private static UserSummary reporterOf(Complaint complaint, Map<Long, UserSummary> reporters) {
        return reporters.getOrDefault(complaint.getReporterId(),
                new UserSummary(complaint.getReporterId(), "Kullanıcı"));
    }
}
