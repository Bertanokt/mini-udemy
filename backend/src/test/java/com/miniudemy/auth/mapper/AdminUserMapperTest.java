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
import com.miniudemy.auth.dto.AdminUserResponse;

/** AdminUserMapper sözleşmedeki AdminUserResponse'u üretir; parola özeti DTO'ya girmez. */
class AdminUserMapperTest {

    private final AdminUserMapper mapper = Mappers.getMapper(AdminUserMapper.class);

    @Test
    void toResponse_copiesAllContractFields() {
        Instant createdAt = Instant.parse("2026-11-14T10:15:30Z");
        User user = new User("ayse@example.com", "$2a$10$ozet", "Ayşe Yılmaz", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "active", false);
        ReflectionTestUtils.setField(user, "createdAt", createdAt);

        assertThat(mapper.toResponse(user)).isEqualTo(
                new AdminUserResponse(42L, "ayse@example.com", "Ayşe Yılmaz", Role.INSTRUCTOR, false, createdAt));
    }

    @Test
    void toResponse_nullUser_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void adminUserResponse_hasExactlyContractFields_andNoPassword() {
        List<String> fields = Arrays.stream(AdminUserResponse.class.getRecordComponents())
                .map(c -> c.getName()).toList();

        assertThat(fields).containsExactly("id", "email", "fullName", "role", "active", "createdAt");
    }
}
