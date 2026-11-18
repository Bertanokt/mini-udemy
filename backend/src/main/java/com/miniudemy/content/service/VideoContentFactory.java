package com.miniudemy.content.service;

import org.springframework.stereotype.Component;

import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.content.domain.VideoContent;
import com.miniudemy.course.domain.LessonContentType;

/** Video dersi fabrikası (#110): ürünü {@link VideoContent}. */
@Component
public class VideoContentFactory extends LessonContentFactory {

    @Override
    public LessonContentType type() {
        return LessonContentType.VIDEO;
    }

    @Override
    protected LessonContent createContent() {
        return new VideoContent();
    }
}
