package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_transfers")
public class TaskTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_employee_id")
    private User fromEmployee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_employee_id", nullable = false)
    private User toEmployee;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transferred_by")
    private User transferredBy;

    @Column(name = "transferred_by_type", length = 30)
    private String transferredByType = "MANAGER";

    @Column(name = "transfer_time", nullable = false, updatable = false)
    private LocalDateTime transferTime;

    @PrePersist
    protected void onCreate() {
        transferTime = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public User getFromEmployee() { return fromEmployee; }
    public void setFromEmployee(User fromEmployee) { this.fromEmployee = fromEmployee; }
    public User getToEmployee() { return toEmployee; }
    public void setToEmployee(User toEmployee) { this.toEmployee = toEmployee; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public User getTransferredBy() { return transferredBy; }
    public void setTransferredBy(User transferredBy) { this.transferredBy = transferredBy; }
    public String getTransferredByType() { return transferredByType; }
    public void setTransferredByType(String transferredByType) { this.transferredByType = transferredByType; }
    public LocalDateTime getTransferTime() { return transferTime; }
}
