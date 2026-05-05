package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AppNotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String body;
    private String projectId;
    private boolean read;
    private LocalDateTime createdAt;
}
