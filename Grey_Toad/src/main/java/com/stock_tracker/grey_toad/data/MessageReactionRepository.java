package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    List<MessageReaction> findByMessageId(UUID messageId);
    List<MessageReaction> findByMessageIdIn(List<UUID> messageIds);
    Optional<MessageReaction> findByMessageIdAndUserId(UUID messageId, UUID userId);
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);
}
