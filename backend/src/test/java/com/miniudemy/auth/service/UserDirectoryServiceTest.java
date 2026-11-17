package com.miniudemy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;

class UserDirectoryServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final UserDirectoryService service = new UserDirectoryService(repository);

    @Test
    void findSummaries_returnsIdAndNameOnly() {
        User user = new User("mehmet@example.com", "hash", "Mehmet Demir", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(user, "id", 7L);
        given(repository.findAllById(List.of(7L, 8L))).willReturn(List.of(user));

        assertThat(service.findSummaries(List.of(7L, 8L)))
                .containsOnlyKeys(7L)
                .containsEntry(7L, new UserSummary(7L, "Mehmet Demir"));
    }

    @Test
    void findSummaries_emptyInput_skipsQuery() {
        assertThat(service.findSummaries(List.of())).isEmpty();

        verifyNoInteractions(repository);
    }
}
