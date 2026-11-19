package com.miniudemy.content.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.common.security.IssuedContentToken;
import com.miniudemy.content.domain.DeliveryMode;
import com.miniudemy.content.dto.ContentDownload;
import com.miniudemy.content.dto.ContentTokenResponse;
import com.miniudemy.content.service.ContentDeliveryService;
import com.miniudemy.content.service.ContentUploadService;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.LessonResponse;

/**
 * İçerik ve kapak uçları (docs/api/openapi.yaml › getLessonContent, getCourseCover; #113; yükleme: uploadLessonContent,
 * uploadCourseCover; #112). Range
 * yalnız ürün destekliyorsa (video): Spring {@link Resource} yanıtında Range'i kendisi uygular (206); desteklemeyen
 * türde gövde {@link InputStreamResource} olarak verilir, Spring bu tipte Range uygulamaz ve dosya 200 ile gider.
 */
@RestController
public class ContentController {

    private final ContentDeliveryService delivery;
    private final ContentUploadService upload;

    public ContentController(ContentDeliveryService delivery, ContentUploadService upload) {
        this.delivery = delivery;
        this.upload = upload;
    }

    @PostMapping("/api/lessons/{id}/content")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public LessonResponse uploadLessonContent(@PathVariable Long id, @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        try (InputStream content = file.getInputStream()) {
            return upload.uploadLessonContent(CurrentUser.id(authentication), id, file.getOriginalFilename(), content);
        }
    }

    @PostMapping("/api/courses/{id}/cover")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseResponse uploadCourseCover(@PathVariable Long id, @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        try (InputStream content = file.getInputStream()) {
            return upload.uploadCourseCover(CurrentUser.id(authentication), id, file.getOriginalFilename(), content);
        }
    }

    /**
     * Ders içeriği. Ya {@code Authorization: Bearer} ile (API istemcisi) ya da {@code ?token=} ile (tarayıcının
     * {@code <video>}/{@code <iframe>} etiketleri başlık gönderemez; ADR-012). Token'lı istek güvenlik filtresinden
     * kimliksiz geçer; token'ı ve yetkiyi servis doğrular.
     */
    @GetMapping("/api/lessons/{id}/content")
    public ResponseEntity<Resource> getLessonContent(@PathVariable Long id,
            @RequestParam(name = "token", required = false) String token, Authentication authentication) {
        if (authentication == null) {
            return respond(delivery.openLessonContentWithToken(id, token));
        }
        return respond(delivery.openLessonContent(id, CurrentUser.id(authentication), isAdmin(authentication)));
    }

    /** İçerik adresi için 5 dakikalık, derse özel token (ADR-012); içeriği açma yetkisiyle aynı kural. */
    @PostMapping("/api/lessons/{id}/content-token")
    public ContentTokenResponse createContentToken(@PathVariable Long id, Authentication authentication) {
        IssuedContentToken issued = delivery.issueContentToken(id, CurrentUser.id(authentication),
                isAdmin(authentication));
        return new ContentTokenResponse(issued.token(), issued.expiresAt());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/api/courses/{id}/cover")
    public ResponseEntity<Resource> getCourseCover(@PathVariable Long id) {
        return respond(delivery.openCourseCover(id));
    }

    private static ResponseEntity<Resource> respond(ContentDownload download) {
        ContentDisposition disposition = (download.deliveryMode() == DeliveryMode.ATTACHMENT
                ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(download.fileName(), StandardCharsets.UTF_8).build();
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (download.rangeRequests()) {
            return response.body(download.resource());
        }
        try {
            return response.header(HttpHeaders.ACCEPT_RANGES, "none").contentLength(download.sizeBytes())
                    .body(new InputStreamResource(download.resource().getInputStream()));
        } catch (IOException e) {
            throw new UncheckedIOException("Dosya okunamadı.", e);
        }
    }
}
