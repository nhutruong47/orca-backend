package org.example.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "task_required_materials", uniqueConstraints = @UniqueConstraint(columnNames = { "task_id", "material_id" }))
public class TaskRequiredMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "required_quantity")
    private Double requiredQuantity = 0.0;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public Double getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(Double requiredQuantity) { this.requiredQuantity = requiredQuantity; }
}
