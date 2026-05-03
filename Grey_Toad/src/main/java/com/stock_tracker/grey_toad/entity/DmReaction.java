package com.stock_tracker.grey_toad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dm_reactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"direct_message_id", "user_id", "emoji"}))
@Getter @Setter
public class DmReaction {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne @JoinColumn(nullable = false, name = "direct_message_id")
    private DirectMessage directMessage;

    @ManyToOne @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false, length = 10)
    private String emoji;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
