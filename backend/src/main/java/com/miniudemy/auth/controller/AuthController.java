package com.miniudemy.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.auth.dto.AuthResponse;
import com.miniudemy.auth.dto.LoginRequest;
import com.miniudemy.auth.dto.PasswordResetConfirmRequest;
import com.miniudemy.auth.dto.PasswordResetRequest;
import com.miniudemy.auth.dto.RegisterRequest;
import com.miniudemy.auth.dto.UserResponse;
import com.miniudemy.auth.service.AuthService;
import com.miniudemy.auth.service.PasswordResetService;

import jakarta.validation.Valid;

/** Kimlik doğrulama uçları (docs/api/openapi.yaml › Auth). Herkese açık: SecurityConfig › PUBLIC_POST. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    /** Öğrenci ya da eğitmen olarak kayıt; 201 + kullanıcı (token dönmez), ADMIN ve hatalı alanlar 400, kayıtlı e-posta 409. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** E-posta ve parola ile giriş; 200 + JWT, yanlış bilgide 401, pasif hesapta 403. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Parola yenileme bağlantısı (#115): e-posta kayıtlı olsun olmasın aynı gövdesiz 202. */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
    }

    /** Token ile yeni parola (#115): 204; geçersiz, süresi dolmuş ya da kullanılmış token 400. */
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
    }
}
