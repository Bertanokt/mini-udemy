package com.miniudemy.quiz.service;

import java.util.Set;

/**
 * Bir sorunun puanlanması için gereken sade girdi: sorunun puanı, doğru şıkların ve öğrencinin seçtiği
 * şıkların id'leri. Entity taşımaz; puanlama stratejileri veritabanına ve JPA'ya bağımlı olmaz.
 */
public record ScoringInput(int points, Set<Long> correctOptionIds, Set<Long> selectedOptionIds) {

    public ScoringInput {
        if (points <= 0) {
            throw new IllegalArgumentException("Soru puanı pozitif olmalı.");
        }
        if (correctOptionIds == null || correctOptionIds.isEmpty()) {
            throw new IllegalArgumentException("Sorunun en az bir doğru şıkkı olmalı.");
        }
        correctOptionIds = Set.copyOf(correctOptionIds);
        selectedOptionIds = selectedOptionIds == null ? Set.of() : Set.copyOf(selectedOptionIds);
    }

    /** Seçilenlerden doğru olanların sayısı. */
    public long correctSelectedCount() {
        return selectedOptionIds.stream().filter(correctOptionIds::contains).count();
    }

    /** Seçilenlerden yanlış olanların sayısı. */
    public long wrongSelectedCount() {
        return selectedOptionIds.size() - correctSelectedCount();
    }
}
