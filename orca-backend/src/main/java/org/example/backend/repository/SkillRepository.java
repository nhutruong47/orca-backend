package org.example.backend.repository;

import org.example.backend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByTeamIdOrderByNameAsc(UUID teamId);
    Optional<Skill> findByTeamIdAndNameIgnoreCase(UUID teamId, String name);
}
