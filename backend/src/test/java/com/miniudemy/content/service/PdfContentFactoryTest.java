package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.miniudemy.content.domain.DeliveryMode;
import com.miniudemy.content.domain.FileFormat;
import com.miniudemy.content.domain.FilePolicy;
import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.content.domain.PdfContent;
import com.miniudemy.course.domain.LessonContentType;

/** PdfContentFactory (#110, Factory Method): türün kuralları ve sunuluşu kendi ürününde (docs/api/openapi.yaml › LessonContentType). */
class PdfContentFactoryTest {

    private final PdfContentFactory factory = new PdfContentFactory();

    @Test
    void servesTheTypePDF() {
        assertThat(factory.type()).isEqualTo(LessonContentType.PDF);
    }

    @Test
    void createsItsOwnProduct() {
        assertThat(factory.create()).isInstanceOf(PdfContent.class);
    }

    @Test
    void filePolicy_allowsOnlyItsFormats_upToItsLimit_withItsMessages() {
        FilePolicy policy = factory.create().filePolicy();

        assertThat(policy.allowedFormats()).containsExactlyInAnyOrder(FileFormat.PDF);
        assertThat(policy.maxBytes()).isEqualTo(FilePolicy.megabytes(20));
        assertThat(policy.tooLargeMessage()).isEqualTo("PDF en fazla 20 MB olabilir.");
        assertThat(policy.unsupportedTypeMessage()).isEqualTo("Yalnız pdf dosyaları yüklenebilir.");
    }

    @Test
    void delivery_isINLINE_rangeRequestsfalse() {
        LessonContent content = factory.create();

        assertThat(content.deliveryMode()).isEqualTo(DeliveryMode.INLINE);
        assertThat(content.supportsRangeRequests()).isEqualTo(false);
    }
}
