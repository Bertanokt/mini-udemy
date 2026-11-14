package com.miniudemy.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserResponse;

/**
 * UserMapper'ın entity'yi sözleşmedeki UserResponse'a doğru çevirdiğini ve parola özetinin DTO'ya hiç
 * girmediğini doğrular. Mapper MapStruct'ın derlemede ürettiği sınıftır; Spring bağlamı gerekmez.
 */
class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toResponse_copiesAllContractFields() {
        Instant createdAt = Instant.parse("2026-11-14T10:15:30Z");
        User user = new User("ayse.yilmaz@example.com", "$2a$10$ozet", "Ayşe Yılmaz", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "bio", "Yazılım mühendisliği öğrencisiyim.");
        ReflectionTestUtils.setField(user, "createdAt", createdAt);

        UserResponse response = mapper.toResponse(user);

        assertThat(response).isEqualTo(new UserResponse(
                42L, "ayse.yilmaz@example.com", "Ayşe Yılmaz", Role.INSTRUCTOR,
                "Yazılım mühendisliği öğrencisiyim.", createdAt));
    }

    @Test
    void toResponse_nullUser_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void userResponse_hasExactlyContractFields_andNoPassword() {
        // docs/api/openapi.yaml › UserResponse ile aynı alanlar; parola özeti, hesap durumu ve güncelleme
        // zamanı API'ye çıkmaz.
        List<String> fields = Arrays.stream(UserResponse.class.getRecordComponents())
                .map(c -> c.getName())
                .toList();

        assertThat(fields).containsExactly("id", "email", "fullName", "role", "bio", "createdAt");
        assertThat(fields).noneMatch(name -> name.toLowerCase().contains("password"));
    }
}
