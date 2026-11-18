package com.miniudemy.content.domain;

import java.util.Set;

/** Video dersi: mp4 / webm, en fazla 200 MB; tarayıcıda oynatılır, ileri sarma için Range desteklenir. */
public final class VideoContent extends LessonContent {

    private static final FilePolicy POLICY = new FilePolicy(Set.of(FileFormat.MP4, FileFormat.WEBM),
            FilePolicy.megabytes(200), "Video en fazla 200 MB olabilir.", "Yalnız mp4 ve webm videolar yüklenebilir.");

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
        return true;
    }
}
