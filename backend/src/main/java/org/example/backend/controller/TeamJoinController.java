package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.TeamJoinDecisionRequest;
import org.example.backend.dto.TeamJoinRequestDTO;
import org.example.backend.service.TeamJoinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/team-join")
@RequiredArgsConstructor
public class TeamJoinController {

    private final TeamJoinService teamJoinService;

    @GetMapping("/team/{teamId}/pending")
    public ResponseEntity<List<TeamJoinRequestDTO>> getPendingRequests(
            @PathVariable UUID teamId,
            Principal principal) {
        return ResponseEntity.ok(teamJoinService.getPendingRequestsForTeam(teamId, principal.getName()));
    }

    @PostMapping("/{requestId}/decision")
    public ResponseEntity<TeamJoinRequestDTO> makeDecision(
            @PathVariable UUID requestId,
            @Valid @RequestBody TeamJoinDecisionRequest request,
            Principal principal) {
        return ResponseEntity.ok(teamJoinService.makeDecision(requestId, request, principal.getName()));
    }
}
