package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TaskNotification {
    private UUID taskId;
    private Integer caseNumber;
    private String taskTitle;
}
