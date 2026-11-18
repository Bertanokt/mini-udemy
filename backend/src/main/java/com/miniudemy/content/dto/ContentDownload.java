package com.miniudemy.content.dto;

import org.springframework.core.io.Resource;

import com.miniudemy.content.domain.DeliveryMode;

/**
 * Sunulacak bir dosya (#113): akış, Content-Type, gösterim adı, boyut ve türün sunuluş kuralı (Factory Method, #110).
 * Diskteki yol ve depolama anahtarı yoktur.
 */
public record ContentDownload(Resource resource, String mimeType, String fileName, long sizeBytes,
        DeliveryMode deliveryMode, boolean rangeRequests) {
}
