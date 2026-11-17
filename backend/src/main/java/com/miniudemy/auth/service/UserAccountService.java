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
import com.miniudemy.common.exception.NotFoundException;

/** Kullanıcı hesapları üzerinde yönetim işlemleri (#60); `admin` modülü users tablosuna yalnız bunun üzerinden erişir. */
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final AdminUserMapper mapper;

    public UserAccountService(UserRepository userRepository, AdminUserMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /** En yeni kayıt önce; eşitlikte id ile sıra sabit kalır. Sorgu sayısı sayfa boyutundan bağımsızdır. */
    @Transactional(readOnly = true)
    public AdminUserPage search(Role role, Boolean active, String q, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<User> result = userRepository.searchForAdmin(role, active, toLikePattern(q), pageable);
        List<AdminUserResponse> content = result.getContent().stream().map(mapper::toResponse).toList();
        return new AdminUserPage(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public AdminUserResponse setActive(Long id, boolean active) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
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
