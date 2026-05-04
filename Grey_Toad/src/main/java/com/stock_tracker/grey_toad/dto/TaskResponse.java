package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TaskResponse {

    private UUID id;
    private Integer caseNumber;
    private String title;
    private String description;
    private String status;
    private UUID assigneeId;
    private String assigneeName;
    private UUID projectId;
    private String projectName;
    private List<String> teamNames;
    private LocalDateTime deadline;
    private LocalDateTime slaDeadline;
    private boolean archived;
    private String priority;
    private String type;
    private String acceptanceStatus;
    private int totalWorkedMinutes;
    private boolean workingSessionActive;
    private LocalDateTime workStartedAt;
}
