package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByTaskId(UUID taskId);
    List<Attachment> findByMessageId(UUID messageId);
}
