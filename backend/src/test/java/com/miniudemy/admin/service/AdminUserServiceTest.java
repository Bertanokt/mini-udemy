package com.miniudemy.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.dto.AdminUserPage;
import com.miniudemy.auth.dto.AdminUserResponse;
import com.miniudemy.auth.service.UserAccountService;
import com.miniudemy.common.exception.ApiException;

/** Yönetici kullanıcı yönetimi akışı (#60): yönetici kuralı ve auth servisine devir. */
class AdminUserServiceTest {

    private final UserAccountService accounts = mock(UserAccountService.class);
    private final AdminUserService service = new AdminUserService(accounts);

    private static AdminUserResponse response(long id, boolean active) {
        return new AdminUserResponse(id, "k" + id + "@example.com", "Kullanıcı " + id, Role.STUDENT, active,
                Instant.parse("2026-11-11T09:30:00Z"));
    }

    @Test
    void list_delegatesFiltersToAuthService() {
        AdminUserPage page = new AdminUserPage(List.of(response(42L, true)), 1, 10, 11, 2);
        given(accounts.search(Role.STUDENT, true, "ay", 1, 10)).willReturn(page);

        assertThat(service.list(Role.STUDENT, true, "ay", 1, 10)).isSameAs(page);
    }

    @Test
    void setStatus_deactivatesAnotherUser() {
        given(accounts.setActive(42L, false)).willReturn(response(42L, false));

        assertThat(service.setStatus(42L, false, 1L).active()).isFalse();
        verify(accounts).setActive(42L, false);
    }

    @Test
    void setStatus_adminDeactivatingOwnAccount_returns409WithoutTouchingUser() {
        assertThatThrownBy(() -> service.setStatus(1L, false, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage(AdminUserService.SELF_DEACTIVATE)
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(accounts);
    }

    @Test
    void setStatus_adminActivatingOwnAccount_isAllowed() {
        given(accounts.setActive(1L, true)).willReturn(response(1L, true));

        assertThat(service.setStatus(1L, true, 1L).active()).isTrue();
    }
}
