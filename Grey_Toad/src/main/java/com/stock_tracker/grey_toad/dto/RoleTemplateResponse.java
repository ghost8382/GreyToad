package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RoleTemplateResponse {
    private UUID id;
    private String name;
    private String permissionLevel;
}
