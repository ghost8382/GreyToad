package com.stock_tracker.grey_toad.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String quote;
    private String status;
}
