package com.miniudemy.admin.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.miniudemy.admin.domain.ComplaintStatus;
import com.miniudemy.admin.dto.ComplaintPage;
import com.miniudemy.admin.dto.ComplaintResponse;
import com.miniudemy.admin.dto.ComplaintStatusRequest;
import com.miniudemy.admin.service.ComplaintService;
import com.miniudemy.common.security.CurrentUser;

/** Yönetici şikâyet uçları (docs/api/openapi.yaml › Admin). {@code /api/admin/**} yalnız ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/complaints")
public class AdminComplaintController {

    private final ComplaintService complaintService;

    public AdminComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public ComplaintPage adminListComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olabilir.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size) {
        return complaintService.list(status, page, size);
    }

    @PutMapping("/{id}/status")
    public ComplaintResponse adminSetComplaintStatus(@PathVariable Long id,
            @Valid @RequestBody ComplaintStatusRequest request, Authentication authentication) {
        return complaintService.setStatus(id, request.status(), CurrentUser.id(authentication));
    }
}
