package com.miniudemy.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.auth.dto.AuthResponse;
import com.miniudemy.auth.dto.LoginRequest;
import com.miniudemy.auth.service.AuthService;

import jakarta.validation.Valid;

/** Kimlik doğrulama uçları (docs/api/openapi.yaml › Auth). Herkese açık: SecurityConfig › PUBLIC_POST. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** E-posta ve parola ile giriş; 200 + JWT, yanlış bilgide 401, pasif hesapta 403. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
