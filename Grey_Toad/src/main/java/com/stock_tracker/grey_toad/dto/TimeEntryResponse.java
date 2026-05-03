package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Builder
public class TimeEntryResponse {
    private UUID id;
    private UUID taskId;
    private UUID userId;
    private String userName;
    private int minutes;
    private String description;
    private LocalDate date;
}
