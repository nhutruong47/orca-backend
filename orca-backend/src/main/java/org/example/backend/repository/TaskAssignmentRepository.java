package org.example.backend.repository;

import org.example.backend.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {
    List<TaskAssignment> findByTaskId(UUID taskId);
    List<TaskAssignment> findByTaskIdAndActiveTrue(UUID taskId);
    List<TaskAssignment> findByWorkerIdAndActiveTrue(UUID workerId);
    List<TaskAssignment> findByTaskIdAndRoleAndActiveTrue(UUID taskId, String role);
}
