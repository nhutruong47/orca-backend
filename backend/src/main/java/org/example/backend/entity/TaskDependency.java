package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_dependencies")
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    private Task dependsOnTask;

    /** FINISH_TO_START, START_TO_START, FINISH_TO_FINISH, START_TO_FINISH */
    @Column(name = "dependency_type", length = 30)
    private String dependencyType = "FINISH_TO_START";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public TaskDependency() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Task getDependsOnTask() { return dependsOnTask; }
    public void setDependsOnTask(Task dependsOnTask) { this.dependsOnTask = dependsOnTask; }

    public String getDependencyType() { return dependencyType; }
    public void setDependencyType(String dependencyType) { this.dependencyType = dependencyType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}