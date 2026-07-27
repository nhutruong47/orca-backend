package org.example.backend.repository;

import org.example.backend.entity.TeamJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, UUID> {
    List<TeamJoinRequest> findByTeamIdAndStatus(UUID teamId, String status);
    Optional<TeamJoinRequest> findByTeamIdAndUserIdAndStatus(UUID teamId, UUID userId, String status);
    List<TeamJoinRequest> findByUserId(UUID userId);
}
