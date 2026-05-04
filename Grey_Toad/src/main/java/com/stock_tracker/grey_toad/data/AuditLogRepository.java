package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
