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
class UserAccountServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserAccountService service =
            new UserAccountService(userRepository, Mappers.getMapper(AdminUserMapper.class));

    private static User user(long id, String email, String name, Role role, boolean active) {
        User u = new User(email, "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "active", active);
        ReflectionTestUtils.setField(u, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        return u;
    }

    @Test
    void search_mapsPageAndPassesFiltersWithPagingAndStableSort() {
        User ayse = user(42L, "ayse@example.com", "Ayşe Yılmaz", Role.INSTRUCTOR, true);
        Page<User> page = new PageImpl<>(List.of(ayse), org.springframework.data.domain.PageRequest.of(1, 10), 11);
        given(userRepository.searchForAdmin(eq(Role.INSTRUCTOR), eq(true), eq("%ayşe%"), any(Pageable.class)))
                .willReturn(page);

        AdminUserPage result = service.search(Role.INSTRUCTOR, true, "  AYŞE ", 1, 10);

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
    void search_withoutFilters_usesMatchAllPatternAndNullFilters() {
        given(userRepository.searchForAdmin(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        AdminUserPage result = service.search(null, null, null, 0, 20);

        assertThat(result.content()).isEmpty();
        verify(userRepository).searchForAdmin(eq(null), eq(null), eq("%"), any(Pageable.class));
    }

    @Test
    void search_blankQuery_isTreatedAsNoSearch() {
        given(userRepository.searchForAdmin(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.search(null, null, "   ", 0, 20);

        verify(userRepository).searchForAdmin(eq(null), eq(null), eq("%"), any(Pageable.class));
    }

    @Test
    void search_escapesLikeWildcardsInQuery() {
        given(userRepository.searchForAdmin(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.search(null, null, "a_b%c\\d", 0, 20);

        verify(userRepository).searchForAdmin(eq(null), eq(null), eq("%a\\_b\\%c\\\\d%"), any(Pageable.class));
    }

    @Test
    void setActive_deactivatesAnotherUser() {
        User ayse = user(42L, "ayse@example.com", "Ayşe Yılmaz", Role.STUDENT, true);
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));

        AdminUserResponse response = service.setActive(42L, false);

        assertThat(ayse.isActive()).isFalse();
        assertThat(response.active()).isFalse();
        assertThat(response.id()).isEqualTo(42L);
    }

    @Test
    void setActive_reactivatesUser() {
        User ayse = user(42L, "ayse@example.com", "Ayşe Yılmaz", Role.STUDENT, false);
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));

        AdminUserResponse response = service.setActive(42L, true);

        assertThat(ayse.isActive()).isTrue();
        assertThat(response.active()).isTrue();
    }

    @Test
    void setActive_unknownUser_returns404() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActive(99L, false))
                .isInstanceOf(ApiException.class)
                .hasMessage("Kullanıcı bulunamadı.")
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(userRepository).findById(99L);
        verifyNoMoreInteractions(userRepository);
    }
}
