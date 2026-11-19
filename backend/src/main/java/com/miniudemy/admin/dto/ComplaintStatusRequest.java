package com.miniudemy.admin.dto;

import jakarta.validation.constraints.NotNull;

import com.miniudemy.admin.domain.ComplaintStatus;

/**
 * Şikâyet durumu isteği (docs/api/openapi.yaml › ComplaintStatusRequest). Sözleşmede yalnız REVIEWED ve REJECTED
 * vardır; OPEN gelirse {@code Complaint.resolve} 400 ve {@code errors.status} döner.
 */
public record ComplaintStatusRequest(@NotNull(message = "Bu alan zorunludur.") ComplaintStatus status) {
}
