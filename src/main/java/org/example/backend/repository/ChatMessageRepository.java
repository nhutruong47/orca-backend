package org.example.backend.repository;

import org.example.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Group messages (recipient is null) ordered by time */
    List<ChatMessage> findByTeamIdAndRecipientIsNullOrderByCreatedAtAsc(UUID teamId);

    /** Direct messages between two users in a team */
    @Query("SELECT m FROM ChatMessage m WHERE m.team.id = :teamId " +
           "AND ((m.sender.id = :userId1 AND m.recipient.id = :userId2) " +
           "OR (m.sender.id = :userId2 AND m.recipient.id = :userId1)) " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findDirectMessages(
            @Param("teamId") UUID teamId,
            @Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2);

    /** Latest DM message per contact for a user */
    @Query(value = "SELECT m.* FROM chat_messages m " +
           "INNER JOIN (SELECT " +
           "  CASE WHEN sender_id = :userId THEN recipient_id ELSE sender_id END AS contact_id, " +
           "  MAX(created_at) AS max_time " +
           "  FROM chat_messages " +
           "  WHERE team_id = :teamId AND recipient_id IS NOT NULL " +
           "  AND (sender_id = :userId OR recipient_id = :userId) " +
           "  GROUP BY CASE WHEN sender_id = :userId THEN recipient_id ELSE sender_id END) latest " +
           "ON m.created_at = latest.max_time " +
           "AND m.team_id = :teamId AND m.recipient_id IS NOT NULL " +
           "AND (m.sender_id = :userId OR m.recipient_id = :userId) " +
           "ORDER BY m.created_at DESC",
           nativeQuery = true)
    List<ChatMessage> findLastDmMessages(
            @Param("teamId") UUID teamId,
            @Param("userId") UUID userId);
}
