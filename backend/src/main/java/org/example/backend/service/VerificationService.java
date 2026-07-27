package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.AdminVerifyRequest;
import org.example.backend.dto.SubmitVerificationRequest;
import org.example.backend.dto.VerificationRequestDTO;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.entity.VerificationRequest;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.VerificationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRequestRepository verificationRepo;
    private final TeamRepository teamRepo;
    private final NotificationService notificationService;

    @Transactional
    public VerificationRequestDTO submitVerification(SubmitVerificationRequest request, User requestedBy) {
        Team team = teamRepo.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Only owner can submit
        if (!team.getOwner().getId().equals(requestedBy.getId())) {
            throw new RuntimeException("Only team owner can submit verification request");
        }

        VerificationRequest vReq = VerificationRequest.builder()
                .team(team)
                .requestedBy(requestedBy)
                .documentUrl(request.getDocumentUrl())
                .status("PENDING")
                .build();

        vReq = verificationRepo.save(vReq);
        return toDTO(vReq);
    }

    @Transactional(readOnly = true)
    public List<VerificationRequestDTO> getPendingVerifications() {
        return verificationRepo.findByStatus("PENDING").stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public VerificationRequestDTO reviewVerification(UUID requestId, AdminVerifyRequest request) {
        VerificationRequest vReq = verificationRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        vReq.setStatus(request.getStatus().toUpperCase());
        vReq.setAdminNote(request.getAdminNote());
        verificationRepo.save(vReq);

        if ("APPROVED".equals(vReq.getStatus())) {
            Team team = vReq.getTeam();
            team.setVerified(true);
            teamRepo.save(team);
            
            notificationService.createAndSend(
                    team.getOwner(),
                    "Giấy phép kinh doanh đã được duyệt",
                    "Xưởng " + team.getName() + " của bạn đã được duyệt và cấp tick xanh hợp lệ.",
                    "/groups",
                    null
            );
        } else if ("REJECTED".equals(vReq.getStatus())) {
            notificationService.createAndSend(
                    vReq.getTeam().getOwner(),
                    "Giấy phép kinh doanh bị từ chối",
                    "Yêu cầu xác minh của xưởng " + vReq.getTeam().getName() + " đã bị từ chối. Lý do: " + request.getAdminNote(),
                    "/groups",
                    null
            );
        }

        return toDTO(vReq);
    }

    private VerificationRequestDTO toDTO(VerificationRequest req) {
        VerificationRequestDTO dto = new VerificationRequestDTO();
        dto.setId(req.getId());
        dto.setTeamId(req.getTeam().getId());
        dto.setTeamName(req.getTeam().getName());
        dto.setRequestedBy(req.getRequestedBy().getFullName());
        dto.setDocumentUrl(req.getDocumentUrl());
        dto.setStatus(req.getStatus());
        dto.setAdminNote(req.getAdminNote());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setUpdatedAt(req.getUpdatedAt());
        return dto;
    }
}
