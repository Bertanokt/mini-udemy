package com.miniudemy.auth.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;

/**
 * Diğer modüllerin kullanıcıyı yalnız id ile gösterebilmesi için herkese açık kullanıcı bilgisi (ad). Entity ya da
 * repository başka modüle çıkmaz; kurs kartındaki eğitmen adı buradan gelir.
 */
@Service
@Transactional(readOnly = true)
public class UserDirectoryService {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Verilen id'lerin özeti tek sorguda; bulunamayan id sonuçta yer almaz. */
    public Map<Long, UserSummary> findSummaries(Collection<Long> userIds) {
        Map<Long, UserSummary> result = new LinkedHashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        for (User user : userRepository.findAllById(userIds)) {
            result.put(user.getId(), new UserSummary(user.getId(), user.getFullName()));
        }
        return result;
    }
}
