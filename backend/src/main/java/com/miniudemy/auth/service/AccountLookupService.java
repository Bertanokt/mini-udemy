package com.miniudemy.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.security.AccountLookup;

/**
 * {@link AccountLookup}'un {@code auth} modülündeki uygulaması: güvenlik altyapısı her kimlikli istekte
 * kullanıcının hâlâ var olup olmadığını, aktifliğini ve güncel rolünü buradan öğrenir (O3).
 */
@Service
@Transactional(readOnly = true)
public class AccountLookupService implements AccountLookup {

    private final UserRepository userRepository;

    public AccountLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<AccountSnapshot> findById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new AccountSnapshot(user.isActive(), user.getRole().name()));
    }
}
