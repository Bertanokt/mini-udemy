package com.miniudemy.content.service;

import org.springframework.stereotype.Component;

import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.content.domain.DocumentContent;
import com.miniudemy.course.domain.LessonContentType;

/** Doküman dersi fabrikası (#110): ürünü {@link DocumentContent}. */
@Component
public class DocumentContentFactory extends LessonContentFactory {

    @Override
    public LessonContentType type() {
        return LessonContentType.DOCUMENT;
    }

    @Override
    protected LessonContent createContent() {
        return new DocumentContent();
    }
}
