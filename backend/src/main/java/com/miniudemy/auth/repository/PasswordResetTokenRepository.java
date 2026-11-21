package com.miniudemy.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.auth.domain.PasswordResetToken;

/** Parola yenileme token'ları (#115). Token özetiyle (SHA-256) aranır; token'ın kendisi saklanmaz. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Token'ı kullanılmış yapar; yalnız hâlâ kullanılmamış ve süresi geçmemişse. Dönen değer 1 değilse token bu
     * arada başka bir istekle kullanıldı ya da süresi doldu: tek kullanım koşulu veritabanında, tek komutla sağlanır.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now"
            + " where t.id = :id and t.usedAt is null and t.expiresAt > :now")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);

    /** Kullanıcının henüz kullanılmamış token'larını geçersiz kılar (yeni istekte ve parola değişince). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateUnused(@Param("userId") Long userId, @Param("now") Instant now);
}
