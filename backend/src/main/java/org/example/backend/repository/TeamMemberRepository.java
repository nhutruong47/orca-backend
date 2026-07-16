package org.example.backend.repository;

import org.example.backend.entity.TeamMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    @EntityGraph(attributePaths = {"user", "team", "jobLabels"})
    List<TeamMember> findByTeamId(UUID teamId);

    @EntityGraph(attributePaths = {"user", "team", "jobLabels"})
    List<TeamMember> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user", "team", "jobLabels"})
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}
