package org.example.backend.repository;

import org.example.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByGoalId(UUID goalId);

    List<Task> findByMemberId(UUID memberId);

    List<Task> findByGoalTeamId(UUID teamId);

    List<Task> findByMemberIdAndStatus(UUID memberId, String status);

    List<Task> findAllByOrderByCreatedAtDesc();
}
