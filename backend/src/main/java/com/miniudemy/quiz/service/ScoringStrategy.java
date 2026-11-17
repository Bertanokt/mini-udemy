package com.miniudemy.quiz.service;

import com.miniudemy.quiz.domain.ScoringType;

/**
 * Bir sorunun puanlama kuralı (Strategy). Yeni kural, bu arayüzü uygulayan yeni bir Spring bean'idir;
 * hangi kuralın kullanılacağını {@link ScoringStrategyResolver} sınavın {@link ScoringType}'ına göre seçer.
 */
public interface ScoringStrategy {

    /** Bu stratejinin karşılık geldiği puanlama tipi; resolver eşlemeyi buradan kurar. */
    ScoringType type();

    /** Soruyu puanlar. Sonuç 0 ile soru puanı arasındadır. */
    QuestionScore score(ScoringInput input);
}
