package org.example.backend.repository;

import org.example.backend.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @EntityGraph(attributePaths = {"goal", "member", "backupMember", "supervisor"})
    List<Task> findByGoalId(UUID goalId);

    @EntityGraph(attributePaths = {"goal", "member", "backupMember", "supervisor"})
    List<Task> findByMemberId(UUID memberId);

    @EntityGraph(attributePaths = {"goal", "member", "backupMember", "supervisor"})
    List<Task> findByGoalTeamId(UUID teamId);

    @EntityGraph(attributePaths = {"goal", "member", "backupMember", "supervisor"})
    List<Task> findByGoalTeamIdIn(Collection<UUID> teamIds);

    @EntityGraph(attributePaths = {"goal", "member", "backupMember", "supervisor"})
    List<Task> findByMemberIdAndStatus(UUID memberId, String status);

    @EntityGraph(attributePaths = {"goal", "member", "backupMember", "supervisor"})
    List<Task> findAllByOrderByCreatedAtDesc();
}
