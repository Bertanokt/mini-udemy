package com.miniudemy.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserResponse;
import com.miniudemy.auth.mapper.UserMapper;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.NotFoundException;

/** Kullanıcının kendi profili (#14: görüntüleme; #116: güncelleme). */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public ProfileService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Kullanıcının kendi profili. Güvenlik filtresi kullanıcının var ve aktif olduğunu zaten doğruladığı için 404
     * pratikte yalnız iki sorgu arasında silinen kullanıcıda görülür (sözleşmede tanımlı).
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
    }
}
