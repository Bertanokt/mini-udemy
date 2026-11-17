package com.miniudemy.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.AuthResponse;
import com.miniudemy.auth.dto.LoginRequest;
import com.miniudemy.auth.dto.RegisterRequest;
import com.miniudemy.auth.dto.UserResponse;
import com.miniudemy.auth.mapper.UserMapper;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.UnauthorizedException;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityMessages;

/**
 * Kayıt (#12, FR-01) ve giriş (#13, FR-02).
 *
 * Kayıt:
 * <ul>
 *   <li>Yönetici hesabı kayıtla oluşturulamaz (seed ile gelir): {@code role=ADMIN} → 400, {@code errors.role}.</li>
 *   <li>BCrypt en fazla 72 bayt işler; daha uzun parola {@code encode}'da istisna fırlatır. {@code @Size(max = 72)}
 *       karakter saydığı için (Türkçe harf 2 bayt) bayt sınırı burada denetlenir → 400, {@code errors.password}.</li>
 *   <li>E-posta büyük/küçük harfe duyarsız tekildir ({@code uk_users_email}, {@code lower(email)}). Önce
 *       {@code existsByEmail} ile bakılır; iki istek aynı anda gelirse veritabanının tekillik kısıtı yakalanır.
 *       İkisi de 409, {@code errors.email}.</li>
 * </ul>
 *
 * Giriş:
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

    private static final int BCRYPT_MAX_BYTES = 72;
    static final String EMAIL_TAKEN = "Bu e-posta adresi zaten kayıtlı.";

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw BadRequestException.forField("role", "Bu rol ile kayıt olunamaz.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw BadRequestException.forField("password",
                    "Parola çok uzun: en fazla 72 bayt olabilir (Türkçe harfler 2 bayt sayılır).");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw emailTaken();
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.fullName(),
                request.role());
        try {
            return userMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // existsByEmail ile kayıt arasında aynı e-posta başka bir istekle kaydedildi.
            throw emailTaken();
        }
    }

    private static ConflictException emailTaken() {
        return new ConflictException(EMAIL_TAKEN, Map.of("email", EMAIL_TAKEN));
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
