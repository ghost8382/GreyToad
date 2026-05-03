package com.stock_tracker.grey_toad.dto;

import com.stock_tracker.grey_toad.entity.MessageType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class MessageResponse {
    private UUID id;
    private String content;
    private UUID senderId;
    private String senderName;
    private UUID channelId;
    private UUID parentId;
    private LocalDateTime createdAt;
    @Builder.Default
    private List<MessageReactionResponse> reactions = List.of();
    @Builder.Default
    private int replyCount = 0;
    @Builder.Default
    private MessageType type = MessageType.CHAT;
}
