package com.miniudemy.common.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Doğrulanmış (imzası ve süresi geçerli) bir JWT'yi Spring Security kimliğine çevirir.
 *
 * İmza ve süre kontrolünü Spring'in {@code BearerTokenAuthenticationFilter}'ı ve {@code JwtDecoder} yapar; bu
 * sınıf ondan sonra çalışır ve kullanıcının <b>güncel</b> durumunu {@link AccountLookup}'a sorar (O3):
 * <ul>
 *   <li>kullanıcı yoksa (silinmiş) → {@link InvalidBearerTokenException} → 401;</li>
 *   <li>pasifse → {@link DisabledException} → 403 "Hesabınız devre dışı bırakılmış.";</li>
 *   <li>aktifse yetki, token'daki {@code role} claim'inden değil veritabanındaki rolden kurulur; rolü değişen
 *       kullanıcının eski token'ı eski rolle çalışmaz.</li>
 * </ul>
 */
public class JwtAccountAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AccountLookup accountLookup;

    public JwtAccountAuthenticationConverter(AccountLookup accountLookup) {
        this.accountLookup = accountLookup;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = parseUserId(jwt.getSubject());
        AccountLookup.AccountSnapshot account = accountLookup.findById(userId)
                .orElseThrow(() -> new InvalidBearerTokenException("Token'daki kullanıcı bulunamadı."));
        if (!account.active()) {
            throw new DisabledException("Kullanıcı hesabı devre dışı.");
        }
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + account.role()));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private static Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidBearerTokenException("Token'da geçerli bir kullanıcı kimliği yok.");
        }
    }
}
