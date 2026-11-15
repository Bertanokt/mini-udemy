package com.miniudemy.auth.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.AuthResponse;
import com.miniudemy.auth.dto.LoginRequest;
import com.miniudemy.auth.mapper.UserMapper;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.UnauthorizedException;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityMessages;

/**
 * Giriş (#13, FR-02). Kayıt #12'de eklenecek.
 *
 * <ul>
 *   <li>Yanlış e-posta ile yanlış parola aynı 401'i ve aynı mesajı döner; hangisinin yanlış olduğu söylenmez.</li>
 *   <li>Kullanıcı bulunamasa da parola, sabit bir özete karşı BCrypt ile karşılaştırılır. Böylece "e-posta yok"
 *       yanıtı "parola yanlış" yanıtından belirgin biçimde hızlı dönmez; yanıt süresi e-postanın kayıtlı olup
 *       olmadığını ele vermez.</li>
 *   <li>Pasif hesap yalnız parola doğruysa 403 alır (sözleşme); parola yanlışsa 401, hesap durumu ele verilmez.</li>
 *   <li>Giriş denemelerinde sınır (rate limiting) yok: kapsam kararı, 15.11.2026 (güvenlik denetimi O4, #82).</li>
 * </ul>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    /** Kullanıcı bulunamadığında karşılaştırılan özet; hiçbir parolaya karşılık gelmez. */
    private final String unknownUserHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(request.email());
        String hash = found.map(User::getPasswordHash).orElse(unknownUserHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException(SecurityMessages.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new ForbiddenException(SecurityMessages.ACCOUNT_DISABLED);
        }
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return AuthResponse.bearer(token, jwtService.expiresInSeconds(), userMapper.toResponse(user));
    }
}
