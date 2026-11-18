package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.miniudemy.content.domain.DocumentContent;
import com.miniudemy.content.domain.PdfContent;
import com.miniudemy.content.domain.VideoContent;
import com.miniudemy.course.domain.LessonContentType;

/** Kayıt, Spring'in bulduğu fabrika bean'leriyle kurulur (liste enjeksiyonu); üç tür de çözülür. Docker gerekmez. */
@SpringJUnitConfig({VideoContentFactory.class, PdfContentFactory.class, DocumentContentFactory.class,
        LessonContentFactories.class})
class LessonContentFactoriesWiringTest {

    @Autowired
    private LessonContentFactories factories;

    @Test
    void springBeansFillTheRegistry() {
        assertThat(factories.forType(LessonContentType.VIDEO).create()).isInstanceOf(VideoContent.class);
        assertThat(factories.forType(LessonContentType.PDF).create()).isInstanceOf(PdfContent.class);
        assertThat(factories.forType(LessonContentType.DOCUMENT).create()).isInstanceOf(DocumentContent.class);
    }
}
