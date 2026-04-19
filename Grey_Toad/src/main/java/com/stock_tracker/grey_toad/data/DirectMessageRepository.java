package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    List<DirectMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
            UUID firstSenderId,
            UUID firstReceiverId,
            UUID secondSenderId,
            UUID secondReceiverId
    );
}
