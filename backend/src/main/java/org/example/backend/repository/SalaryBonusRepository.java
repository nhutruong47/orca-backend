package org.example.backend.repository;

import org.example.backend.entity.SalaryBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryBonusRepository extends JpaRepository<SalaryBonus, UUID> {
    
    // Find all bonuses for a team in a specific month
    List<SalaryBonus> findByTeamIdAndYearAndMonth(UUID teamId, Integer year, Integer month);

    // Find bonus for a specific user in a specific team and month
    Optional<SalaryBonus> findByTeamIdAndUserIdAndYearAndMonth(UUID teamId, UUID userId, Integer year, Integer month);

    // Find all bonuses for a user in a team (all time)
    List<SalaryBonus> findByTeamIdAndUserId(UUID teamId, UUID userId);
    
    // Find all bonuses for a team (all time)
    List<SalaryBonus> findByTeamId(UUID teamId);
}
