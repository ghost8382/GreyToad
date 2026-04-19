package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppNotificationDto {
    private String type;  // TASK_ASSIGNED | DM | CHANNEL_MESSAGE
    private String title;
    private String body;
}
