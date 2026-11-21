package com.miniudemy.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.domain.PasswordResetToken;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.PasswordResetConfirmRequest;
import com.miniudemy.auth.repository.PasswordResetTokenRepository;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.BadRequestException;

/**
 * Parola yenileme (#115, FR-03; ince kapsam 17.11).
 *
 * <ul>
 *   <li><b>İstek:</b> e-posta kayıtlı ve hesap aktifse kullanıcının eski, kullanılmamış token'ları geçersiz kılınır,
 *       yeni token üretilip {@link PasswordResetNotifier}'a verilir. Kayıtlı olmayan ya da pasif hesapta hiçbir şey
 *       yapılmaz; controller her durumda aynı 202'yi döner, böylece kullanıcının varlığı sızmaz.</li>
 *   <li><b>Token:</b> 32 rastgele bayt ({@link SecureRandom}), URL'ye uygun Base64. Veritabanına yalnız SHA-256
 *       özeti yazılır; süre {@code app.password-reset.ttl} (varsayılan 30 dk).</li>
 *   <li><b>Onay:</b> token bulunamazsa, süresi dolmuşsa ya da kullanılmışsa 400 ({@code errors.token}). Tek kullanım
 *       {@link PasswordResetTokenRepository#markUsed} koşullu güncellemesiyle sağlanır: aynı token'la gelen iki
 *       istekten yalnız biri parolayı değiştirir.</li>
 * </ul>
 *
 * Kapsam dışı: e-posta gönderimi, yeniden gönderme, deneme sınırı.
 */
@Service
@EnableConfigurationProperties(PasswordResetProperties.class)
public class PasswordResetService {

    static final String INVALID_TOKEN = "Bağlantı geçersiz ya da süresi dolmuş. Yeni bir bağlantı isteyin.";
    private static final int TOKEN_BYTES = 32;
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetNotifier notifier;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
            PasswordResetNotifier notifier, PasswordEncoder passwordEncoder, PasswordResetProperties properties,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.notifier = notifier;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** Kayıtlı ve aktif kullanıcıya yenileme bağlantısı üretir; diğer durumlarda sessizce hiçbir şey yapmaz. */
    @Transactional
    public void requestReset(String email) {
        Optional<User> user = users.findByEmail(email.strip()).filter(User::isActive);
        if (user.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.ttl());
        String token = newToken();
        tokens.invalidateUnused(user.get().getId(), now);
        tokens.save(new PasswordResetToken(user.get().getId(), hash(token), now, expiresAt));
        notifier.sendResetLink(user.get().getEmail(), properties.linkBase() + "/reset-password?token=" + token,
                expiresAt);
    }

    /** Token geçerliyse parolayı değiştirir ve token'ı kullanılmış yapar. */
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw BadRequestException.forField("newPassword",
                    "Parola çok uzun: en fazla 72 bayt olabilir (Türkçe harfler 2 bayt sayılır).");
        }
        Instant now = clock.instant();
        PasswordResetToken token = tokens.findByTokenHash(hash(request.token()))
                .filter(t -> t.isUsableAt(now))
                .orElseThrow(PasswordResetService::invalidToken);
        if (tokens.markUsed(token.getId(), now) != 1) {
            throw invalidToken();
        }
        User user = users.findById(token.getUserId())
                .filter(User::isActive)
                .orElseThrow(PasswordResetService::invalidToken);
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        tokens.invalidateUnused(user.getId(), now);
    }

    /** Token adresten gelir, formda alan değildir; kullanıcıya üst mesajda da asıl sebep görünsün. */
    private static BadRequestException invalidToken() {
        return new BadRequestException(INVALID_TOKEN, Map.of("token", INVALID_TOKEN));
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256, küçük harf hex (64 karakter); veritabanındaki {@code token_hash}. */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 bulunamadı.", e);
        }
    }
}
