package com.miniudemy.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.auth.dto.UpdateProfileRequest;
import com.miniudemy.auth.dto.UserResponse;
import com.miniudemy.auth.service.ProfileService;
import com.miniudemy.common.security.CurrentUser;

import jakarta.validation.Valid;

/** Giriş yapmış kullanıcının kendi kaynakları (docs/api/openapi.yaml › Users). */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Token'daki kullanıcının profili. Her rol çağırabilir; token yoksa 401 (SecurityConfig › anyRequest). */
    @GetMapping
    public UserResponse getMyProfile(Authentication authentication) {
        return profileService.getMyProfile(CurrentUser.id(authentication));
    }

    /** Ad soyad ve kısa biyografi (#116); e-posta ya da rol gönderilirse 400 (alan hatasıyla). */
    @PutMapping
    public UserResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        return profileService.updateMyProfile(CurrentUser.id(authentication), request);
    }
}
