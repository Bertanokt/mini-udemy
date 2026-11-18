package com.miniudemy.content.domain;

import java.util.Set;

/**
 * Doküman dersi: pdf, docx ya da pptx, en fazla 20 MB; indirilir. Sunuluşu dosyanın biçimi değil dersin türü belirler:
 * doküman olarak yüklenen bir PDF de indirilir (ADR-011).
 */
public final class DocumentContent extends LessonContent {

    private static final FilePolicy POLICY = new FilePolicy(Set.of(FileFormat.PDF, FileFormat.DOCX, FileFormat.PPTX),
            FilePolicy.megabytes(20), "Doküman en fazla 20 MB olabilir.",
            "Yalnız pdf, docx ve pptx dokümanlar yüklenebilir.");

    @Override
    public FilePolicy filePolicy() {
        return POLICY;
    }

    @Override
    public DeliveryMode deliveryMode() {
        return DeliveryMode.ATTACHMENT;
    }

    @Override
    public boolean supportsRangeRequests() {
        return false;
    }
}
