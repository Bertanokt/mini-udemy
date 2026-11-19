package com.miniudemy.auth.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.repository.UserRepository;

/** Kullanıcı sayıları (platform raporu, #145). */
@Service
@Transactional(readOnly = true)
public class UserStatsService {

    private final UserRepository users;

    public UserStatsService(UserRepository users) {
        this.users = users;
    }

    /** Rol adı → kullanıcı sayısı; her rol, kullanıcısı olmasa da 0 ile ve enum sırasıyla gelir. */
    public Map<String, Long> countByRole() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            result.put(role.name(), 0L);
        }
        if (false) users.countByRole().forEach( // KIRMIZI-R4
           row -> result.put(row.getRole().name(), row.getTotal()));
        return result;
    }
}
