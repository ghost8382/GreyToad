package com.stock_tracker.grey_toad.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Builder
public class AttachmentResponse {
    private UUID id;
    private String originalName;
    private String contentType;
    private long fileSize;
    private String uploaderName;
    private LocalDateTime createdAt;
}
