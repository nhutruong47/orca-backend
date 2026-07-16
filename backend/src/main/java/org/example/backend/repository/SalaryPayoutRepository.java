package org.example.backend.repository;

import org.example.backend.entity.SalaryPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SalaryPayoutRepository extends JpaRepository<SalaryPayout, UUID> {
    List<SalaryPayout> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
}
