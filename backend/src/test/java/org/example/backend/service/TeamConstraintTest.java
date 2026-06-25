package org.example.backend.service;

import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TeamConstraintTest {

    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private GoalRepository goalRepo;
    @Autowired private TaskRepository taskRepo;
    @Autowired private TeamMemberRepository teamMemberRepo;

    @PersistenceContext
    private EntityManager entityManager;

    private User owner;
    private Team team;
    private Goal goal;
    private Task task;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUsername("owner_delete_" + UUID.randomUUID());
        owner.setPassword("pass");
        owner = userRepo.save(owner);

        team = new Team();
        team.setName("Team to Delete");
        team.setOwner(owner);
        team = teamRepo.save(team);

        TeamMember tm = new TeamMember();
        tm.setTeam(team);
        tm.setUser(owner);
        teamMemberRepo.save(tm);

        goal = new Goal();
        goal.setTitle("Delete Goal");
        goal.setTeam(team);
        goal.setOwner(owner);
        goal = goalRepo.save(goal);

        task = new Task();
        task.setTitle("Delete Task");
        task.setGoal(goal);
        task = taskRepo.save(task);
    }

    @Test
    void deleteTeam_ShouldCascadeDeleteGoalsAndTasks() {
        // Assert initial state
        assertTrue(teamRepo.findById(team.getId()).isPresent());
        assertTrue(goalRepo.findById(goal.getId()).isPresent());
        assertTrue(taskRepo.findById(task.getId()).isPresent());
        assertFalse(teamMemberRepo.findByTeamId(team.getId()).isEmpty());

        // Clear persistence context so managed entities don't cause TransientObjectException on flush
        entityManager.clear();

        // Delete team
        teamRepo.deleteById(team.getId());
        teamRepo.flush();

        // Assert cascade deletion
        assertFalse(teamRepo.findById(team.getId()).isPresent());
        
        // Goals and Tasks should be deleted by DB cascade if configured correctly,
        // or by JPA. We just check if they still exist.
        // NOTE: In testing with H2, OnDelete CASCADE works.
        assertFalse(goalRepo.findById(goal.getId()).isPresent(), "Goal should be cascaded");
        assertFalse(taskRepo.findById(task.getId()).isPresent(), "Task should be cascaded");
        assertTrue(teamMemberRepo.findByTeamId(team.getId()).isEmpty(), "Team members should be cascaded");
    }
}
