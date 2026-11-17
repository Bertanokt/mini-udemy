package com.miniudemy.quiz.dto;

/** Öğrenci görünümündeki şık; yalnız id ve metin, doğru olup olmadığı yoktur. */
public record StudentOptionResponse(Long id, String text) {
}
