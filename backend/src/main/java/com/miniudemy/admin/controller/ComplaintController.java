package com.miniudemy.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.miniudemy.admin.dto.ComplaintResponse;
import com.miniudemy.admin.dto.CreateComplaintRequest;
import com.miniudemy.admin.service.ComplaintService;
import com.miniudemy.common.security.CurrentUser;

/** Şikâyet oluşturma (docs/api/openapi.yaml › Complaints). Giriş yapmış her kullanıcı; rol kısıtı yok. */
@RestController
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping("/api/complaints")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse createComplaint(@Valid @RequestBody CreateComplaintRequest request,
            Authentication authentication) {
        return complaintService.create(CurrentUser.id(authentication), request);
    }
}
