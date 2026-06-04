package org.example.backend.repository;

import org.example.backend.entity.TaskRequiredMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRequiredMaterialRepository extends JpaRepository<TaskRequiredMaterial, UUID> {
    List<TaskRequiredMaterial> findByTaskId(UUID taskId);
}
