package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter @Builder
public class MessageReactionResponse {
    private String emoji;
    private int count;
    private boolean reactedByMe;
    private List<String> reactors;
}
