package com.miniudemy.course.domain;

import com.miniudemy.common.exception.ConflictException;

/**
 * Kursun şu anki durumunda izinli olmayan bir işlem (State deseni, #85): standart hata gövdesiyle 409 döner.
 *
 * Domain'in {@code common.exception}'a tek bağımlılığı budur; gerekçesi ve ArchUnit kurallarıyla ilişkisi
 * ADR-010'da.
 */
public class CourseStateException extends ConflictException {

    public CourseStateException(String message) {
        super(message);
    }
}
