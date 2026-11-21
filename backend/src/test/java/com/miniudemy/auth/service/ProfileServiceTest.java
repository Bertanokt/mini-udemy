package com.miniudemy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UpdateProfileRequest;
import com.miniudemy.auth.dto.UserResponse;
import com.miniudemy.auth.mapper.UserMapper;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.ApiException;

/** Kendi profilini görüntüleme (#14) ve güncelleme (#116). */
class ProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileService profileService =
            new ProfileService(userRepository, Mappers.getMapper(UserMapper.class));

    @Test
    void getMyProfile_returnsUserResponseOfThatUser() {
        User user = new User("ayse.yilmaz@example.com", "$2a$10$ozet", "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "bio", "Yazılım mühendisliği öğrencisiyim.");
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        given(userRepository.findById(42L)).willReturn(Optional.of(user));

        assertThat(profileService.getMyProfile(42L)).isEqualTo(new UserResponse(42L, "ayse.yilmaz@example.com",
                "Ayşe Yılmaz", Role.STUDENT, "Yazılım mühendisliği öğrencisiyim.", Instant.parse("2026-11-11T09:30:00Z")));
    }

    @Test
    void getMyProfile_whenUserMissing_returns404WithContractMessage() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getMyProfile(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Kullanıcı bulunamadı.")
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateMyProfile_changesOnlyNameAndBio_trimmed_emailAndRoleUnchanged() {
        User user = new User("ayse.yilmaz@example.com", "$2a$10$ozet", "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(user, "id", 42L);
        given(userRepository.findById(42L)).willReturn(Optional.of(user));
        given(userRepository.saveAndFlush(user)).willReturn(user);

        UserResponse updated = profileService.updateMyProfile(42L,
                new UpdateProfileRequest("  Ayşe Yılmaz Kaya ", " Veri bilimine meraklıyım. ", null, null));

        assertThat(updated.fullName()).isEqualTo("Ayşe Yılmaz Kaya");
        assertThat(updated.bio()).isEqualTo("Veri bilimine meraklıyım.");
        assertThat(updated.email()).isEqualTo("ayse.yilmaz@example.com");
        assertThat(updated.role()).isEqualTo(Role.STUDENT);
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$ozet");
    }

    @Test
    void updateMyProfile_blankOrNullBio_clearsIt() {
        User user = new User("ayse.yilmaz@example.com", "$2a$10$ozet", "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "bio", "Eski biyografi");
        given(userRepository.findById(42L)).willReturn(Optional.of(user));
        given(userRepository.saveAndFlush(user)).willReturn(user);

        assertThat(profileService.updateMyProfile(42L, new UpdateProfileRequest("Ayşe", "   ", null, null)).bio())
                .isNull();
        ReflectionTestUtils.setField(user, "bio", "Eski biyografi");
        assertThat(profileService.updateMyProfile(42L, new UpdateProfileRequest("Ayşe", null, null, null)).bio())
                .isNull();
    }

    @Test
    void updateMyProfile_whenUserMissing_returns404() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateMyProfile(99L,
                        new UpdateProfileRequest("Ayşe", null, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
