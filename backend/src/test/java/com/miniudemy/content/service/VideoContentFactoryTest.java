package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.miniudemy.content.domain.DeliveryMode;
import com.miniudemy.content.domain.FileFormat;
import com.miniudemy.content.domain.FilePolicy;
import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.content.domain.VideoContent;
import com.miniudemy.course.domain.LessonContentType;

/** VideoContentFactory (#110, Factory Method): türün kuralları ve sunuluşu kendi ürününde (docs/api/openapi.yaml › LessonContentType). */
class VideoContentFactoryTest {

    private final VideoContentFactory factory = new VideoContentFactory();

    @Test
    void servesTheTypeVIDEO() {
        assertThat(factory.type()).isEqualTo(LessonContentType.VIDEO);
    }

    @Test
    void createsItsOwnProduct() {
        assertThat(factory.create()).isInstanceOf(VideoContent.class);
    }

    @Test
    void filePolicy_allowsOnlyItsFormats_upToItsLimit_withItsMessages() {
        FilePolicy policy = factory.create().filePolicy();

        assertThat(policy.allowedFormats()).containsExactlyInAnyOrder(FileFormat.MP4, FileFormat.WEBM);
        assertThat(policy.maxBytes()).isEqualTo(FilePolicy.megabytes(200));
        assertThat(policy.tooLargeMessage()).isEqualTo("Video en fazla 200 MB olabilir.");
        assertThat(policy.unsupportedTypeMessage()).isEqualTo("Yalnız mp4 ve webm videolar yüklenebilir.");
    }

    @Test
    void delivery_isINLINE_rangeRequeststrue() {
        LessonContent content = factory.create();

        assertThat(content.deliveryMode()).isEqualTo(DeliveryMode.INLINE);
        assertThat(content.supportsRangeRequests()).isEqualTo(true);
    }
}
