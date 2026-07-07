package org.example.backend.repository;

import org.example.backend.entity.CostCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface CostCategoryRepository extends JpaRepository<CostCategory, UUID> {
    Optional<CostCategory> findByName(String name);
    List<CostCategory> findAllByOrderByCreatedAtDesc();
}
