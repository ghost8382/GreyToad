package com.stock_tracker.grey_toad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_reactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "emoji"}))
@Getter @Setter
public class MessageReaction {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne @JoinColumn(nullable = false, name = "message_id")
    private Message message;

    @ManyToOne @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false, length = 10)
    private String emoji;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
