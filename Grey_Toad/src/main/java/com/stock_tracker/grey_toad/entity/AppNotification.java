package com.stock_tracker.grey_toad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_notifications")
@Getter
@Setter
public class AppNotification {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID recipientId;

    private String type; // TASK_ASSIGNED | DM | MENTION

    @Column(length = 200)
    private String title;

    @Column(length = 400)
    private String body;

    private String projectId;

    @Column(columnDefinition = "boolean default false")
    private boolean read = false;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
