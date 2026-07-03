package org.example.backend.controller;

import org.example.backend.entity.Task;
import org.example.backend.entity.TaskChecklist;
import org.example.backend.entity.User;
import org.example.backend.repository.TaskChecklistRepository;
import org.example.backend.repository.TaskRepository;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract test: ensures the front-end singular /checklist/ and /api/tasks/checklist/... aliases
 * resolve to existing controllers (no 404 / 405).
 *
 * We accept any 4xx except 404/405; this proves the route exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TaskChecklistContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TaskRepository taskRepo;
    @Autowired private TaskChecklistRepository checklistRepo;
    @Autowired private UserRepository userRepo;

    @Test
    @WithMockUser(username = "contract-user", roles = "MEMBER")
    void singular_checklist_alias_route_exists() throws Exception {
        UUID taskId = UUID.randomUUID();
        int status = mockMvc.perform(get("/api/tasks/" + taskId + "/checklist"))
                .andReturn().getResponse().getStatus();
        // Accept 4xx (forbidden) or 2xx — but never 404 / 405
        if (status == 404 || status == 405) {
            throw new AssertionError("Expected route to exist; got " + status);
        }
    }

    @Test
    @WithMockUser(username = "contract-user", roles = "MEMBER")
    void singular_checklist_toggle_alias_route_exists() throws Exception {
        UUID checklistId = UUID.randomUUID();
        int status = mockMvc.perform(patch("/api/tasks/checklist/" + checklistId + "/toggle"))
                .andReturn().getResponse().getStatus();
        if (status == 404 || status == 405) {
            throw new AssertionError("Expected route to exist; got " + status);
        }
    }
}