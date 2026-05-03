package com.stock_tracker.grey_toad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter @Setter
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne @JoinColumn
    private User actor;

    @ManyToOne @JoinColumn
    private Project project;

    @Column(nullable = false)
    private String action;       // TASK_CREATED, STATUS_CHANGED, ASSIGNED, PRIORITY_CHANGED, COMMENT_ADDED, ARCHIVED

    @Column(nullable = false)
    private String entityType;   // TASK, COMMENT

    private String entityId;
    private String details;      // human-readable description

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
