package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    List<AppNotification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(UUID recipientId);

    @Modifying
    @Query("UPDATE AppNotification n SET n.read = true WHERE n.recipientId = :rid AND n.read = false")
    void markAllReadByRecipient(@Param("rid") UUID recipientId);

    @Modifying
    @Query("UPDATE AppNotification n SET n.read = true WHERE n.id = :id")
    void markOneRead(@Param("id") UUID id);
}
