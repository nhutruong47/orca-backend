package org.example.backend.controller;

import org.example.backend.dto.TeamDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.TeamJoinService;
import org.example.backend.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TeamControllerTest {

    private TeamService teamService;
    private TeamJoinService teamJoinService;
    private AccessControlService accessControlService;
    private TeamController teamController;
    private Authentication auth;
    private User testUser;

    @BeforeEach
    void setUp() {
        teamService = mock(TeamService.class);
        teamJoinService = mock(TeamJoinService.class);
        accessControlService = mock(AccessControlService.class);
        teamController = new TeamController(teamService, teamJoinService, accessControlService);
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    }

    @Test
    void getMyTeams_ReturnsOk() {
        when(teamService.getTeamsForUser("testuser")).thenReturn(List.of());
        ResponseEntity<?> response = teamController.getMyTeams(auth);
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void getTeamDetail_ReturnsOk() {
        UUID teamId = UUID.randomUUID();
        doNothing().when(accessControlService).requireTeamMember(testUser, teamId);
        when(teamService.getTeamDetail(teamId)).thenReturn(new TeamDTO());

        ResponseEntity<?> response = teamController.getTeamDetail(teamId, testUser);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void createTeam_ReturnsOk() {
        TeamDTO dto = new TeamDTO();
        when(teamService.createTeam(any(TeamDTO.class), eq("testuser"))).thenReturn(dto);

        ResponseEntity<TeamDTO> response = teamController.createTeam(dto, auth);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void joinByCode_ReturnsOk() {
        TeamDTO dto = new TeamDTO();
        when(teamService.joinByCode("ABCDEF", "testuser")).thenReturn(dto);

        ResponseEntity<?> response = teamController.joinByCode(Map.of("inviteCode", "ABCDEF"), auth);

        assertEquals(200, response.getStatusCodeValue());
    }
}
