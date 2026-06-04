package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "worker_skills", uniqueConstraints = @UniqueConstraint(columnNames = { "team_member_id", "skill_id" }))
public class WorkerSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_member_id", nullable = false)
    private TeamMember teamMember;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        if (level == null) level = 1;
        level = Math.max(1, Math.min(5, level));
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TeamMember getTeamMember() { return teamMember; }
    public void setTeamMember(TeamMember teamMember) { this.teamMember = teamMember; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
