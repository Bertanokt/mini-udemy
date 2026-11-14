package com.miniudemy.common.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniudemy.common.exception.ErrorResponse;

/**
 * Güvenlik filtresinde oluşan 401 ve 403'leri standart {@link ErrorResponse} gövdesiyle yazar.
 *
 * Bu hatalar istek controller'a ulaşmadan oluşur, bu yüzden {@code GlobalExceptionHandler}'a hiç gelmez:
 * <ul>
 *   <li>{@link AuthenticationEntryPoint}: token yok, geçersiz, süresi dolmuş ya da kullanıcı silinmiş → 401;
 *       kullanıcı pasifse ({@link DisabledException}) → 403 "Hesabınız devre dışı bırakılmış." (sözleşmedeki
 *       {@code Forbidden › deactivated} örneği).</li>
 *   <li>{@link AccessDeniedHandler}: adres kuralıyla rol yetersiz (ör. kursiyer {@code /api/admin/**}) → 403.</li>
 * </ul>
 */
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        if (authException instanceof DisabledException) {
            write(response, HttpStatus.FORBIDDEN, SecurityMessages.ACCOUNT_DISABLED);
        } else {
            response.setHeader("WWW-Authenticate", "Bearer");
            write(response, HttpStatus.UNAUTHORIZED, SecurityMessages.UNAUTHORIZED);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, SecurityMessages.FORBIDDEN);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(status.value(), message));
    }
}
