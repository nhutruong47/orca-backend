package org.example.backend.repository;

import org.example.backend.entity.TaskTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskTransferRepository extends JpaRepository<TaskTransfer, UUID> {
    List<TaskTransfer> findByTaskIdOrderByTransferTimeDesc(UUID taskId);
}
