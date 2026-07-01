package org.example.backend.repository;

import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByChipId(String chipId);

    Boolean existsByUsername(String username);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.aiUsageCount = u.aiUsageCount + 1 WHERE u.id = :userId AND u.aiUsageCount < :limit")
    int incrementAiUsageIfUnderLimit(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("limit") int limit);
}