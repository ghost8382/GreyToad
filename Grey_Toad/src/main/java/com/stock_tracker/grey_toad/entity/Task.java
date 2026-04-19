package com.stock_tracker.grey_toad.entity;



import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String status; // TODO, IN_PROGRESS, DONE

    @ManyToOne
    private User assignee;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Project project;

    private LocalDateTime deadline;

    @Column(name = "case_number")
    private Integer caseNumber;

    @Column(columnDefinition = "boolean default false")
    private boolean archived = false;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(columnDefinition = "varchar(20) default 'MEDIUM'")
    private String priority = "MEDIUM"; // CRITICAL | HIGH | MEDIUM | LOW

    @Column(columnDefinition = "varchar(20) default 'TASK'")
    private String type = "TASK"; // BUG | FEATURE | TASK | STORY
}
