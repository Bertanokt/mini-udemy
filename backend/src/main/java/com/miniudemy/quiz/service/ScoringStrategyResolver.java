package com.miniudemy.quiz.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.miniudemy.quiz.domain.ScoringType;

/**
 * Sınavın {@link ScoringType}'ına göre puanlama stratejisini verir. Eşleme, bean'lerin kendi {@code type()}
 * değerinden kurulur; seçimde if/switch yoktur. Bir tip için strateji yoksa ya da iki strateji aynı tipi
 * iddia ediyorsa uygulama başlarken hata verir.
 */
@Component
public class ScoringStrategyResolver {

    private final Map<ScoringType, ScoringStrategy> strategies = new EnumMap<>(ScoringType.class);

    public ScoringStrategyResolver(List<ScoringStrategy> candidates) {
        for (ScoringStrategy strategy : candidates) {
            ScoringStrategy previous = strategies.put(strategy.type(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Aynı puanlama tipi için birden fazla strateji var: " + strategy.type());
            }
        }
        for (ScoringType type : ScoringType.values()) {
            if (!strategies.containsKey(type)) {
                throw new IllegalStateException("Puanlama tipi için strateji tanımlı değil: " + type);
            }
        }
    }

    public ScoringStrategy resolve(ScoringType type) {
        return strategies.get(type);
    }
}
