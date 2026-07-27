package org.example.backend.repository;

import org.example.backend.entity.VerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {
    List<VerificationRequest> findByTeamId(UUID teamId);
    List<VerificationRequest> findByStatus(String status);
}
