package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    List<DirectMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
            UUID firstSenderId,
            UUID firstReceiverId,
            UUID secondSenderId,
            UUID secondReceiverId
    );

    @Query("SELECT dm.sender.id, COUNT(dm) FROM DirectMessage dm WHERE dm.receiver.id = :receiverId AND dm.read = false GROUP BY dm.sender.id")
    List<Object[]> countUnreadGroupedBySender(@Param("receiverId") UUID receiverId);

    @Modifying
    @Query("UPDATE DirectMessage dm SET dm.read = true WHERE dm.sender.id = :senderId AND dm.receiver.id = :receiverId AND dm.read = false")
    void markConversationRead(@Param("senderId") UUID senderId, @Param("receiverId") UUID receiverId);
}
