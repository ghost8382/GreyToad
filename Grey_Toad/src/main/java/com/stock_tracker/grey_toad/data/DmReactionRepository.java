package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.DmReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DmReactionRepository extends JpaRepository<DmReaction, UUID> {
    List<DmReaction> findByDirectMessageId(UUID directMessageId);
    List<DmReaction> findByDirectMessageIdIn(List<UUID> ids);
    Optional<DmReaction> findByDirectMessageIdAndUserId(UUID directMessageId, UUID userId);
    Optional<DmReaction> findByDirectMessageIdAndUserIdAndEmoji(UUID directMessageId, UUID userId, String emoji);
}
