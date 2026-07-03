package org.example.backend.repository;

import org.example.backend.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {
    List<TaskDependency> findByTaskId(UUID taskId);
}