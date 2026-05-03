package com.stock_tracker.grey_toad.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
public class CreateTimeEntryRequest {

    @NotNull
    private UUID userId;

    @Min(1)
    private int minutes;

    private String description;

    @NotNull
    private LocalDate date;
}
