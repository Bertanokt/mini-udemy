package com.miniudemy.admin.domain;

/** Şikâyetin durumu (docs/api/openapi.yaml › ComplaintStatus). Şikâyet OPEN doğar; yönetici sonuçlandırır. */
public enum ComplaintStatus {
    OPEN,
    REVIEWED,
    REJECTED
}
