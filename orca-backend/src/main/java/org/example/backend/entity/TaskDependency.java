package org.example.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "task_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = { "task_id", "depends_on_task_id" }))
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    private Task dependsOnTask;

    @Column(name = "dependency_type", nullable = false, length = 40)
    private String dependencyType = "FINISH_TO_START";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public Task getDependsOnTask() { return dependsOnTask; }
    public void setDependsOnTask(Task dependsOnTask) { this.dependsOnTask = dependsOnTask; }
    public String getDependencyType() { return dependencyType; }
    public void setDependencyType(String dependencyType) { this.dependencyType = dependencyType; }
}
