package com.miniudemy.content.service;

import org.springframework.stereotype.Component;

import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.content.domain.PdfContent;
import com.miniudemy.course.domain.LessonContentType;

/** PDF dersi fabrikası (#110): ürünü {@link PdfContent}. */
@Component
public class PdfContentFactory extends LessonContentFactory {

    @Override
    public LessonContentType type() {
        return LessonContentType.PDF;
    }

    @Override
    protected LessonContent createContent() {
        return new PdfContent();
    }
}
