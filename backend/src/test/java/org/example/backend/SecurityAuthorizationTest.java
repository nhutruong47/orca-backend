package org.example.backend;

import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.AccessControlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class SecurityAuthorizationTest {

    @Autowired
    private AccessControlService accessControlService;

    @Test
    public void testRequireTeamMemberThrowsForbiddenWhenNotInTeam() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UUID teamId = UUID.randomUUID();

        assertThrows(ResponseStatusException.class, () -> {
            accessControlService.requireTeamMember(user, teamId);
        });
    }
}
