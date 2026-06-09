package org.example.backend.service;

import org.example.backend.dto.ai.AiExtractRequest;
import org.example.backend.dto.ai.AiExtractResponse;
import org.example.backend.dto.ai.AiPlanDraftResponse;
import org.example.backend.dto.ai.AiPlanRequest;
import org.example.backend.dto.ai.AiReviseRequest;
import org.example.backend.dto.ai.AiTeamMemberContext;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AiWorkflowService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.v2.service-url:http://127.0.0.1:8000}")
    private String aiServiceUrl;

    public AiWorkflowService(TeamMemberRepository teamMemberRepository, TeamRepository teamRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
    }

    public AiExtractResponse extract(AiExtractRequest request, User currentUser) {
        validateText(request != null ? request.getText() : null);
        if (request.getTeamId() != null && !request.getTeamId().isBlank()) {
            validateTeamAccess(parseTeamId(request.getTeamId()), currentUser);
        }
        return post("/extract", request, AiExtractResponse.class);
    }

    public AiPlanDraftResponse plan(AiPlanRequest request, User currentUser) {
        UUID teamId = requireTeamId(request != null ? request.getTeamId() : null);
        validateTeamAccess(teamId, currentUser);
        request.setMembers(loadTeamMembers(teamId));
        return post("/plan", request, AiPlanDraftResponse.class);
    }

    public AiPlanDraftResponse revise(AiReviseRequest request, User currentUser) {
        UUID teamId = requireTeamId(request != null ? request.getTeamId() : null);
        validateTeamAccess(teamId, currentUser);
        request.setMembers(loadTeamMembers(teamId));
        return post("/revise", request, AiPlanDraftResponse.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            T response = restTemplate.postForObject(aiServiceUrl + path, body, responseType);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned empty response");
            }
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cannot reach AI service: " + e.getMessage(), e);
        }
    }

    private List<AiTeamMemberContext> loadTeamMembers(UUID teamId) {
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(this::toContext)
                .toList();
    }

    private AiTeamMemberContext toContext(TeamMember teamMember) {
        User user = teamMember.getUser();
        AiTeamMemberContext context = new AiTeamMemberContext();
        context.setUserId(user.getId().toString());
        context.setUsername(user.getUsername());
        context.setFullName(user.getFullName());
        context.setJobLabels(teamMember.getJobLabels() != null ? teamMember.getJobLabels() : Collections.emptyList());
        return context;
    }

    private void validateText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text is required");
        }
    }

    private UUID requireTeamId(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId is required");
        }
        return parseTeamId(teamId);
    }

    private UUID parseTeamId(String teamId) {
        try {
            return UUID.fromString(teamId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid teamId", e);
        }
    }

    private void validateTeamAccess(UUID teamId, User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");
        }
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
        }
    }
}
