package com.miniudemy.admin.service;

import org.springframework.stereotype.Service;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.dto.AdminUserPage;
import com.miniudemy.auth.dto.AdminUserResponse;
import com.miniudemy.auth.service.UserAccountService;
import com.miniudemy.common.exception.ConflictException;

/**
 * Yönetici kullanıcı yönetimi akışı (#60). users tablosuna erişim auth modülünün
 * {@link UserAccountService}'indedir; burada yalnız yönetici kuralı durur.
 */
@Service
public class AdminUserService {

    static final String SELF_DEACTIVATE = "Kendi hesabınızı pasifleştiremezsiniz.";

    private final UserAccountService userAccountService;

    public AdminUserService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public AdminUserPage list(Role role, Boolean active, String q, int page, int size) {
        return userAccountService.search(role, active, q, page, size);
    }

    /** Yönetici kendi hesabını pasifleştiremez; kendini yeniden aktifleştirmek serbest. */
    public AdminUserResponse setStatus(Long id, boolean active, Long currentAdminId) {
        if (!active && id.equals(currentAdminId)) {
            throw new ConflictException(SELF_DEACTIVATE);
        }
        return userAccountService.setActive(id, active);
    }
}
