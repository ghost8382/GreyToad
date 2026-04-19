package com.stock_tracker.grey_toad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChannelRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String teamId;
}