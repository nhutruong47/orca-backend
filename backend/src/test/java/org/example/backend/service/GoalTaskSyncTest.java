package org.example.backend.service;

import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GoalTaskSyncTest {

    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepo;
    @Autowired private GoalRepository goalRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;

    private Goal goal;
    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setUsername("owner_sync_" + UUID.randomUUID());
        owner.setPassword("pass");
        owner = userRepo.save(owner);

        Team team = new Team();
        team.setName("Sync Team");
        team.setOwner(owner);
        team = teamRepo.save(team);

        goal = new Goal();
        goal.setTitle("Sync Goal");
        goal.setTeam(team);
        goal.setOwner(owner);
        goal = goalRepo.save(goal);

        task1 = new Task();
        task1.setTitle("Task 1");
        task1.setGoal(goal);
        task1.setStatus("PENDING");
        task1 = taskRepo.save(task1);

        task2 = new Task();
        task2.setTitle("Task 2");
        task2.setGoal(goal);
        task2.setStatus("PENDING");
        task2 = taskRepo.save(task2);
    }

    @Test
    void whenAllTasksCompleted_GoalStatusIsDone() {
        taskService.updateStatus(task1.getId(), "COMPLETED");
        
        Goal updatedGoal = goalRepo.findById(goal.getId()).orElseThrow();
        assertEquals(2, updatedGoal.getTotalTasks());
        assertEquals(1, updatedGoal.getCompletedTasks());
        assertEquals("IN_PROGRESS", updatedGoal.getStatus());

        taskService.updateStatus(task2.getId(), "COMPLETED");
        
        updatedGoal = goalRepo.findById(goal.getId()).orElseThrow();
        assertEquals(2, updatedGoal.getCompletedTasks());
        assertEquals("DONE", updatedGoal.getStatus());
    }

    @Test
    void whenTaskRevertsToPending_GoalStatusRevertsFromDone() {
        taskService.updateStatus(task1.getId(), "COMPLETED");
        taskService.updateStatus(task2.getId(), "COMPLETED");
        
        Goal updatedGoal = goalRepo.findById(goal.getId()).orElseThrow();
        assertEquals("DONE", updatedGoal.getStatus());

        // Revert task 1
        taskService.updateStatus(task1.getId(), "PENDING");
        
        updatedGoal = goalRepo.findById(goal.getId()).orElseThrow();
        assertEquals(1, updatedGoal.getCompletedTasks());
        assertEquals("IN_PROGRESS", updatedGoal.getStatus());
    }
}
