package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private String quote;
    private String status;
}