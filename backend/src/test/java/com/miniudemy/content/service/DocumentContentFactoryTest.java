package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.miniudemy.content.domain.DeliveryMode;
import com.miniudemy.content.domain.FileFormat;
import com.miniudemy.content.domain.FilePolicy;
import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.content.domain.DocumentContent;
import com.miniudemy.course.domain.LessonContentType;

/** DocumentContentFactory (#110, Factory Method): türün kuralları ve sunuluşu kendi ürününde (docs/api/openapi.yaml › LessonContentType). */
class DocumentContentFactoryTest {

    private final DocumentContentFactory factory = new DocumentContentFactory();

    @Test
    void servesTheTypeDOCUMENT() {
        assertThat(factory.type()).isEqualTo(LessonContentType.DOCUMENT);
    }

    @Test
    void createsItsOwnProduct() {
        assertThat(factory.create()).isInstanceOf(DocumentContent.class);
    }

    @Test
    void filePolicy_allowsOnlyItsFormats_upToItsLimit_withItsMessages() {
        FilePolicy policy = factory.create().filePolicy();

        assertThat(policy.allowedFormats()).containsExactlyInAnyOrder(FileFormat.PDF, FileFormat.DOCX, FileFormat.PPTX);
        assertThat(policy.maxBytes()).isEqualTo(FilePolicy.megabytes(20));
        assertThat(policy.tooLargeMessage()).isEqualTo("Doküman en fazla 20 MB olabilir.");
        assertThat(policy.unsupportedTypeMessage()).isEqualTo("Yalnız pdf, docx ve pptx dokümanlar yüklenebilir.");
    }

    @Test
    void delivery_isATTACHMENT_rangeRequestsfalse() {
        LessonContent content = factory.create();

        assertThat(content.deliveryMode()).isEqualTo(DeliveryMode.ATTACHMENT);
        assertThat(content.supportsRangeRequests()).isEqualTo(false);
    }
}
