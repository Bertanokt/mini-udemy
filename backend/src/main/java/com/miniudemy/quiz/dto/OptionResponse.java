package com.miniudemy.quiz.dto;

/** Eğitmen görünümündeki şık; doğru olup olmadığı bilgisini içerir (docs/api/openapi.yaml › OptionResponse). */
public record OptionResponse(Long id, String text, boolean correct) {
}
