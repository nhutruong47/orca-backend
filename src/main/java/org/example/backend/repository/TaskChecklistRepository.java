package org.example.backend.repository;

import org.example.backend.entity.TaskChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskChecklistRepository extends JpaRepository<TaskChecklist, UUID> {
    List<TaskChecklist> findByTaskIdOrderBySortOrderAsc(UUID taskId);
}
