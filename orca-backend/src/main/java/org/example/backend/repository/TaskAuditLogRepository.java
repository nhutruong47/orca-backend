package org.example.backend.repository;

import org.example.backend.entity.TaskAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAuditLogRepository extends JpaRepository<TaskAuditLog, UUID> {
    List<TaskAuditLog> findByTaskIdOrderByCreatedAtDesc(UUID taskId);
}
