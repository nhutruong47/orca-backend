package org.example.backend.service;

import org.example.backend.dto.TeamDTO;
import org.example.backend.entity.Role;
import org.example.backend.entity.Team;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamServiceJoinTest {

    @Test
    void joinByCodeAddsCurrentUserAsMember() {
        TeamService service = new TeamService();
        TeamRepository teamRepository = mock(TeamRepository.class);
        TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        User owner = User.builder()
                .id(UUID.randomUUID())
                .username("owner")
                .password("secret")
                .role(Role.MEMBER)
                .build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("worker")
                .password("secret")
                .role(Role.MEMBER)
                .build();

        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setName("Roastery");
        team.setOwner(owner);
        team.setInviteCode("ABC123");
        ReflectionTestUtils.setField(team, "createdAt", LocalDateTime.now());

        when(teamRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(team));
        when(userRepository.findByUsername("worker")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId())).thenReturn(Optional.empty());
        when(teamMemberRepository.findByTeamId(team.getId())).thenReturn(List.of());

        ReflectionTestUtils.setField(service, "teamRepository", teamRepository);
        ReflectionTestUtils.setField(service, "teamMemberRepository", teamMemberRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        TeamDTO joined = service.joinByCode("abc123", "worker");

        assertEquals(team.getId(), joined.getId());
        assertEquals("Roastery", joined.getName());
        verify(teamMemberRepository).save(any(TeamMember.class));
    }
}
