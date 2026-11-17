package com.miniudemy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.AdminUserPage;
import com.miniudemy.auth.dto.AdminUserResponse;
import com.miniudemy.auth.mapper.AdminUserMapper;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.ApiException;

/** Yönetici kullanıcı yönetimi (#60). */
class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminUserService service =
            new AdminUserService(userRepository, Mappers.getMapper(AdminUserMapper.class));

    private static User user(long id, String email, String name, Role role, boolean active) {
        User u = new User(email, "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "active", active);
        ReflectionTestUtils.setField(u, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        return u;
    }

    @Test
    void list_mapsPageAndPassesFiltersWithPagingAndStableSort() {
        User ayse = user(42L, "ayse@example.com", "Ayşe Yılmaz", Role.INSTRUCTOR, true);
        Page<User> page = new PageImpl<>(List.of(ayse), org.springframework.data.domain.PageRequest.of(1, 10), 11);
        given(userRepository.searchForAdmin(eq(Role.INSTRUCTOR), eq(true), eq("%ayşe%"), any(Pageable.class)))
                .willReturn(page);

        AdminUserPage result = service.list(Role.INSTRUCTOR, true, "  AYŞE ", 1, 10);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(11);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content()).containsExactly(new AdminUserResponse(42L, "ayse@example.com", "Ayşe Yılmaz",
                Role.INSTRUCTOR, true, Instant.parse("2026-11-11T09:30:00Z")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchForAdmin(any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");
    }

    @Test
    void list_withoutFilters_usesMatchAllPatternAndNullFilters() {
        given(userRepository.searchForAdmin(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        AdminUserPage result = service.list(null, null, null, 0, 20);

        assertThat(result.content()).isEmpty();
        verify(userRepository).searchForAdmin(eq(null), eq(null), eq("%"), any(Pageable.class));
    }

    @Test
    void list_blankQuery_isTreatedAsNoSearch() {
        given(userRepository.searchForAdmin(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.list(null, null, "   ", 0, 20);

        verify(userRepository).searchForAdmin(eq(null), eq(null), eq("%"), any(Pageable.class));
    }

    @Test
    void list_escapesLikeWildcardsInQuery() {
        given(userRepository.searchForAdmin(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.list(null, null, "a_b%c\\d", 0, 20);

        verify(userRepository).searchForAdmin(eq(null), eq(null), eq("%a\\_b\\%c\\\\d%"), any(Pageable.class));
    }

    @Test
    void setStatus_deactivatesAnotherUser() {
        User ayse = user(42L, "ayse@example.com", "Ayşe Yılmaz", Role.STUDENT, true);
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));

        AdminUserResponse response = service.setStatus(42L, false, 1L);

        assertThat(ayse.isActive()).isFalse();
        assertThat(response.active()).isFalse();
        assertThat(response.id()).isEqualTo(42L);
    }

    @Test
    void setStatus_reactivatesUser() {
        User ayse = user(42L, "ayse@example.com", "Ayşe Yılmaz", Role.STUDENT, false);
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));

        AdminUserResponse response = service.setStatus(42L, true, 1L);

        assertThat(ayse.isActive()).isTrue();
        assertThat(response.active()).isTrue();
    }

    @Test
    void setStatus_adminDeactivatingOwnAccount_returns409AndKeepsActive() {
        User admin = user(1L, "admin@example.com", "Yönetici", Role.ADMIN, true);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.setStatus(1L, false, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage(AdminUserService.SELF_DEACTIVATE)
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    void setStatus_adminActivatingOwnAccount_isAllowed() {
        User admin = user(1L, "admin@example.com", "Yönetici", Role.ADMIN, true);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        assertThat(service.setStatus(1L, true, 1L).active()).isTrue();
    }

    @Test
    void setStatus_unknownUser_returns404() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStatus(99L, false, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Kullanıcı bulunamadı.")
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(userRepository).findById(99L);
        verifyNoMoreInteractions(userRepository);
    }
}
