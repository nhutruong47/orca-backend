package org.example.backend.service;

import org.example.backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TeamServiceJoinTest {

    @Test
    void joinByCodeShouldBeDisabledForSecurity() {
        TeamService service = new TeamService();
        TeamRepository teamRepository = mock(TeamRepository.class);

        ReflectionTestUtils.setField(service, "teamRepository", teamRepository);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.joinByCode("ABC123", "someone"));

        assert ex.getMessage().contains("đã bị vô hiệu hóa");
    }
}
