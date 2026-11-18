package com.miniudemy.content.domain;

import java.util.Set;

/** Kurs kapak görselinin yükleme kuralı (#112): jpg / png / webp, en fazla 2 MB. */
public final class CoverImage {

    public static final FilePolicy POLICY = new FilePolicy(
            Set.of(FileFormat.JPEG, FileFormat.PNG, FileFormat.WEBP), FilePolicy.megabytes(2),
            "Kapak görseli en fazla 2 MB olabilir.", "Yalnız jpg, png ve webp görseller yüklenebilir.");

    private CoverImage() {
    }
}
