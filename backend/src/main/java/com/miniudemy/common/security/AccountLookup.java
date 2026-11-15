package com.miniudemy.common.security;

import java.util.Optional;

/**
 * Token'daki kullanıcının güncel durumunu sorar: hâlâ var mı, aktif mi, rolü ne?
 *
 * Arayüz {@code common}'da tanımlı, uygulaması {@code auth} modülünde: güvenlik altyapısı {@code auth}'u
 * bilmez, bağımlılık yönü hep {@code auth → common} kalır (Dependency Inversion; eğitmen istatistiklerindeki
 * kalıbın aynısı, docs/architecture.md › Modüller arası iletişim).
 *
 * Her kimlikli istekte çağrılır; böylece pasifleştirilen ya da rolü değişen kullanıcının eski token'ı süresi
 * dolmadan da geçersiz olur (güvenlik denetimi O3).
 */
public interface AccountLookup {

    Optional<AccountSnapshot> findById(Long userId);

    /** Kullanıcının istek anındaki durumu. {@code role} rol adıdır (örn. {@code STUDENT}). */
    record AccountSnapshot(boolean active, String role) {
    }
}
