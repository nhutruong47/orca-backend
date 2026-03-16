package org.example.backend.repository;

import org.example.backend.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOwnerId(UUID ownerId);

    Optional<Team> findByInviteCode(String inviteCode);
}
