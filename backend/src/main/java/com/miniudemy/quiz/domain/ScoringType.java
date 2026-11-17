package com.miniudemy.quiz.domain;

/**
 * Sınavın puanlama tipi. Değerler `quizzes.scoring_type` kolonundaki CHECK kısıtıyla
 * ({@code ck_quizzes_scoring_type}) ve OpenAPI'deki `ScoringType` ile aynıdır. Her değerin hesabını yapan
 * Strategy sınıfları #52'dedir; burada yalnız saklanan değer vardır.
 */
public enum ScoringType {
    /** Tek doğru şık: doğruysa sorunun puanı. */
    SINGLE_ANSWER,
    /** Tüm doğru şıklar ve yalnız onlar seçildiyse tam puan. */
    MULTI_ALL_OR_NOTHING,
    /** Doğru seçilen şık oranında puan, yanlış seçim puanı düşürür (en az 0). */
    MULTI_PARTIAL
}
