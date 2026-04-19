package com.stock_tracker.grey_toad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateTaskRequest {

    @NotNull
    private String title;

    private String description;

    @NotNull
    private UUID projectId;

    private UUID assigneeId;

    private String status;

    private boolean autoAssign;

    private String priority; // CRITICAL | HIGH | MEDIUM | LOW

    private String type;     // BUG | FEATURE | TASK | STORY
}