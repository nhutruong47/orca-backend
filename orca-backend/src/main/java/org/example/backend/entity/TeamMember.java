package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "team_members", uniqueConstraints = @UniqueConstraint(columnNames = { "team_id", "user_id" }))
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_role", nullable = false)
    private GroupRole groupRole = GroupRole.MEMBER;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "team_member_labels", joinColumns = @JoinColumn(name = "team_member_id"))
    @Column(name = "label")
    private List<String> jobLabels = new ArrayList<>();

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }

    public TeamMember() {
    }

    // === Getters & Setters ===
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GroupRole getGroupRole() {
        return groupRole;
    }

    public void setGroupRole(GroupRole groupRole) {
        this.groupRole = groupRole;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public List<String> getJobLabels() {
        return jobLabels;
    }

    public void setJobLabels(List<String> jobLabels) {
        this.jobLabels = jobLabels;
    }
}
