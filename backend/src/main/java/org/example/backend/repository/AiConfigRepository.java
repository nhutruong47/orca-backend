package org.example.backend.repository;

import org.example.backend.entity.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConfigRepository extends JpaRepository<AiConfig, String> {
}
