package com.miniudemy.admin.dto;

import java.util.List;

/** Sayfalı şikâyet listesi (docs/api/openapi.yaml › ComplaintPage). {@code page} 0'dan başlar. */
public record ComplaintPage(
        List<ComplaintResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
