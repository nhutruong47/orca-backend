package org.example.backend.service;

import org.example.backend.dto.TaskDTO;
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
public class TaskServiceIntegrationTest {

    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepo;
    @Autowired private GoalRepository goalRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private TeamMemberRepository teamMemberRepo;

    private User owner;
    private User stranger;
    private Team team;
    private Goal goal;
    private Task task;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUsername("owner_" + UUID.randomUUID());
        owner.setPassword("pass");
        owner = userRepo.save(owner);

        stranger = new User();
        stranger.setUsername("stranger_" + UUID.randomUUID());
        stranger.setPassword("pass");
        stranger = userRepo.save(stranger);

        team = new Team();
        team.setName("Test Team");
        team.setOwner(owner);
        team = teamRepo.save(team);

        // Add owner to team members
        TeamMember tm = new TeamMember();
        tm.setTeam(team);
        tm.setUser(owner);
        teamMemberRepo.save(tm);

        goal = new Goal();
        goal.setTitle("Test Goal");
        goal.setTeam(team);
        goal.setOwner(owner);
        goal = goalRepo.save(goal);

        task = new Task();
        task.setTitle("Test Task");
        task.setGoal(goal);
        task = taskRepo.save(task);
    }

    @Test
    void assignTaskToNonMember_ShouldThrowException() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            taskService.assign(task.getId(), stranger.getId());
        });
        
        assertTrue(exception.getMessage().contains("Người dùng không thuộc xưởng này"));
    }

    @Test
    void assignTaskToMember_ShouldSucceed() {
        TaskDTO dto = taskService.assign(task.getId(), owner.getId());
        assertNotNull(dto);
        assertEquals(owner.getId().toString(), dto.getMemberId());
    }
}
