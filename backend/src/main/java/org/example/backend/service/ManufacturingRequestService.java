package org.example.backend.service;

import org.example.backend.dto.ManufacturingRequestDTO;
import org.example.backend.entity.ManufacturingRequest;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.ManufacturingRequestRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ManufacturingRequestService {

    private final ManufacturingRequestRepository repo;
    private final TeamRepository teamRepo;

    public ManufacturingRequestService(ManufacturingRequestRepository repo, TeamRepository teamRepo) {
        this.repo = repo;
        this.teamRepo = teamRepo;
    }

    public List<ManufacturingRequestDTO> getAllRequests() {
        return repo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ManufacturingRequestDTO createRequest(ManufacturingRequestDTO dto, User user) {
        ManufacturingRequest req = new ManufacturingRequest();
        req.setBuyerUser(user);
        
        if (dto.getBuyerTeamId() != null && !dto.getBuyerTeamId().isBlank()) {
            Team team = teamRepo.findById(UUID.fromString(dto.getBuyerTeamId())).orElse(null);
            req.setBuyerTeam(team);
        }

        req.setType(dto.getType());
        req.setTitle(dto.getTitle());
        req.setCoffeeType(dto.getCoffeeType());
        req.setQuantity(dto.getQuantity());
        req.setDeadline(dto.getDeadline());
        req.setRegion(dto.getRegion());
        req.setDetails(dto.getDetails());

        return toDTO(repo.save(req));
    }

    private ManufacturingRequestDTO toDTO(ManufacturingRequest req) {
        ManufacturingRequestDTO dto = new ManufacturingRequestDTO();
        dto.setId(req.getId().toString());
        dto.setType(req.getType());
        dto.setTitle(req.getTitle());
        dto.setCoffeeType(req.getCoffeeType());
        dto.setQuantity(req.getQuantity());
        dto.setDeadline(req.getDeadline());
        dto.setRegion(req.getRegion());
        dto.setDetails(req.getDetails());
        dto.setCreatedAt(req.getCreatedAt() != null ? req.getCreatedAt().toString() : null);
        if (req.getBuyerTeam() != null) {
            dto.setBuyerTeamId(req.getBuyerTeam().getId().toString());
        }
        return dto;
    }
}
