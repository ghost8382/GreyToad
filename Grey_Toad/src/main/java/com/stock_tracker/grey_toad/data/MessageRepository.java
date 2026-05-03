package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.Message;
import com.stock_tracker.grey_toad.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChannelIdOrderByCreatedAtAsc(UUID channelId);
    List<Message> findByChannelId(UUID channelId);
    List<Message> findByParentIdOrderByCreatedAtAsc(UUID parentId);
    List<Message> findByChannelIdAndTypeOrderByCreatedAtDesc(UUID channelId, MessageType type);

    void deleteByChannelId(UUID channelId);
}
