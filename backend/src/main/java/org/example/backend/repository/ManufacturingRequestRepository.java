package org.example.backend.repository;

import org.example.backend.entity.ManufacturingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ManufacturingRequestRepository extends JpaRepository<ManufacturingRequest, UUID> {
    List<ManufacturingRequest> findAllByOrderByCreatedAtDesc();
}
