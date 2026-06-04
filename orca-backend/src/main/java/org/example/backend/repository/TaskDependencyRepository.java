package org.example.backend.repository;

import org.example.backend.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {
    List<TaskDependency> findByTaskId(UUID taskId);
    List<TaskDependency> findByDependsOnTaskId(UUID dependsOnTaskId);
    Optional<TaskDependency> findByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);
}
