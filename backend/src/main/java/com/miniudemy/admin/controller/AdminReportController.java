package com.miniudemy.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.admin.dto.PlatformReportResponse;
import com.miniudemy.admin.service.PlatformReportService;

/**
 * Platform raporu (docs/api/openapi.yaml › adminGetPlatformReport, #145). Yalnız ADMIN: {@code /api/admin/**} adres
 * kuralıyla korunur (SecurityConfig).
 */
@RestController
public class AdminReportController {

    private final PlatformReportService reports;

    public AdminReportController(PlatformReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/api/admin/reports")
    public PlatformReportResponse adminGetPlatformReport() {
        return reports.report();
    }
}
