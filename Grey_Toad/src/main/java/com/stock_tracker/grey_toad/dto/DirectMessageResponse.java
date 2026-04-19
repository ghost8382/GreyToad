package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DirectMessageResponse {

    private UUID id;
    private String content;
    private UUID senderId;
    private UUID receiverId;
    private LocalDateTime createdAt;
}