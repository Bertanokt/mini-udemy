package com.miniudemy.content.service;

import java.util.Objects;

import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.course.domain.LessonContentType;

/**
 * Factory Method'un yaratıcısı (#110, ADR-011). Her ders içerik türünün kendi alt sınıfı vardır ve ürününü
 * {@link #createContent()} ile kendisi üretir. Çağıranlar (yükleme #112, sunum #113) yalnız bu arayüzü ve
 * {@link LessonContent}'i bilir; yeni bir tür = yeni bir fabrika sınıfı, çağıranlarda if/switch yok.
 *
 * Fabrikalar Spring bean'idir; tür → fabrika seçimi {@link LessonContentFactories}'te.
 */
public abstract class LessonContentFactory {

    /** Bu fabrikanın ürettiği ders içerik türü. */
    public abstract LessonContentType type();

    /** Factory method: türün ürünü. */
    protected abstract LessonContent createContent();

    /** Türün ürünü; çağıranların kullandığı tek giriş. */
    public final LessonContent create() {
        return Objects.requireNonNull(createContent(), () -> getClass().getSimpleName() + " ürün üretmedi");
    }
}
