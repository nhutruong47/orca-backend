package org.example.backend.repository;

import org.example.backend.entity.WorkerSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerSkillRepository extends JpaRepository<WorkerSkill, UUID> {
    List<WorkerSkill> findByTeamMemberTeamId(UUID teamId);
    List<WorkerSkill> findByTeamMemberId(UUID teamMemberId);
    Optional<WorkerSkill> findByTeamMemberIdAndSkillId(UUID teamMemberId, UUID skillId);
}
