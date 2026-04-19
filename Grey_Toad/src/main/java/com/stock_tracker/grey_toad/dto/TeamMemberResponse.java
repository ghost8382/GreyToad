package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TeamMemberResponse {

    private UUID id;
    private UUID userId;
    private UUID teamId;
    private String role;
}