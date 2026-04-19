package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CommentResponse {

    private UUID id;
    private String content;
    private UUID authorId;
    private UUID taskId;
    private LocalDateTime createdAt;
}