package org.example.backend.repository;

import org.example.backend.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOwnerId(UUID ownerId);

    Optional<Team> findByInviteCode(String inviteCode);

    @EntityGraph(attributePaths = {"owner"})
    @Query("SELECT t FROM Team t WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.owner.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Team> searchTeams(@Param("search") String search, Pageable pageable);
}
