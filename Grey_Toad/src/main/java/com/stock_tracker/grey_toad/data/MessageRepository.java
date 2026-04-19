package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChannelId(UUID channelId);

    void deleteByChannelId(UUID channelId);
}
