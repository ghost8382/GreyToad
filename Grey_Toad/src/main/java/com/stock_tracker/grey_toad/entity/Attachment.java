package com.stock_tracker.grey_toad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter @Setter
public class Attachment {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedName;   // UUID-based filename on disk

    @Column(nullable = false)
    private String contentType;

    private long fileSize;

    // nullable — belongs to task OR channel message
    @ManyToOne @JoinColumn
    private Task task;

    @ManyToOne @JoinColumn
    private Message message;

    @ManyToOne @JoinColumn(nullable = false)
    private User uploader;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
