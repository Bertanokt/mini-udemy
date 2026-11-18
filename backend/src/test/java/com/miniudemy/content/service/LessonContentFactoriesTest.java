package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.course.domain.LessonContentType;

/** Tür → fabrika seçimi tek yerde, if/switch olmadan; her türün tam bir fabrikası olmalı, yoksa uygulama başlamaz. */
class LessonContentFactoriesTest {

    private final LessonContentFactories factories = new LessonContentFactories(
            List.of(new VideoContentFactory(), new PdfContentFactory(), new DocumentContentFactory()));

    @ParameterizedTest
    @EnumSource(LessonContentType.class)
    void everyTypeHasItsFactory(LessonContentType type) {
        assertThat(factories.forType(type).type()).isEqualTo(type);
    }

    @Test
    void missingFactoryForAType_failsAtStartup() {
        assertThatThrownBy(() -> new LessonContentFactories(List.of(new VideoContentFactory(), new PdfContentFactory())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DOCUMENT");
    }

    @Test
    void twoFactoriesForTheSameType_failAtStartup() {
        LessonContentFactory secondVideo = new LessonContentFactory() {
            @Override
            public LessonContentType type() {
                return LessonContentType.VIDEO;
            }

            @Override
            protected LessonContent createContent() {
                return new VideoContentFactory().create();
            }
        };

        assertThatThrownBy(() -> new LessonContentFactories(List.of(new VideoContentFactory(), new PdfContentFactory(),
                new DocumentContentFactory(), secondVideo)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VIDEO");
    }

    @Test
    void nullType_isRefused() {
        assertThatThrownBy(() -> factories.forType(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
