package com.miniudemy.content.domain;

import java.util.Set;

/** PDF dersi: yalnız pdf, en fazla 20 MB; tarayıcıda açılır. */
public final class PdfContent extends LessonContent {

    private static final FilePolicy POLICY = new FilePolicy(Set.of(FileFormat.PDF), FilePolicy.megabytes(20),
            "PDF en fazla 20 MB olabilir.", "Yalnız pdf dosyaları yüklenebilir.");

    @Override
    public FilePolicy filePolicy() {
        return POLICY;
    }

    @Override
    public DeliveryMode deliveryMode() {
        return DeliveryMode.INLINE;
    }

    @Override
    public boolean supportsRangeRequests() {
        return false;
    }
}
