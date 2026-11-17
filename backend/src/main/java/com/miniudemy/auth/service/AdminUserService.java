package com.miniudemy.auth.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.AdminUserPage;
import com.miniudemy.auth.dto.AdminUserResponse;
import com.miniudemy.auth.mapper.AdminUserMapper;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;

/** Yönetici kullanıcı yönetimi (#60): listeleme/arama ve hesabı aktifleştirme/pasifleştirme. */
@Service
public class AdminUserService {

    static final String SELF_DEACTIVATE = "Kendi hesabınızı pasifleştiremezsiniz.";

    private final UserRepository userRepository;
    private final AdminUserMapper mapper;

    public AdminUserService(UserRepository userRepository, AdminUserMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /** En yeni kayıt önce; eşitlikte id ile sıra sabit kalır. Sorgu sayısı sayfa boyutundan bağımsızdır. */
    @Transactional(readOnly = true)
    public AdminUserPage list(Role role, Boolean active, String q, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<User> result = userRepository.searchForAdmin(role, active, toLikePattern(q), pageable);
        List<AdminUserResponse> content = result.getContent().stream().map(mapper::toResponse).toList();
        return new AdminUserPage(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public AdminUserResponse setStatus(Long id, boolean active, Long currentAdminId) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
        if (!active && user.getId().equals(currentAdminId)) {
            throw new ConflictException(SELF_DEACTIVATE);
        }
        user.setActive(active);
        return mapper.toResponse(user);
    }

    private static String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String escaped = q.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
