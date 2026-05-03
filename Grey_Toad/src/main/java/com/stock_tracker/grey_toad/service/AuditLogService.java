package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.AuditLogRepository;
import com.stock_tracker.grey_toad.data.ProjectRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.AuditLogResponse;
import com.stock_tracker.grey_toad.entity.AuditLog;
import com.stock_tracker.grey_toad.entity.Project;
import com.stock_tracker.grey_toad.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           ProjectRepository projectRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    public void log(String actorEmail, UUID projectId, String action,
                    String entityType, String entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);

        userRepository.findByEmail(actorEmail).ifPresent(entry::setActor);

        if (projectId != null) {
            projectRepository.findById(projectId).ifPresent(entry::setProject);
        }

        auditLogRepository.save(entry);
    }

    public List<AuditLogResponse> getByProject(UUID projectId) {
        return auditLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(e -> AuditLogResponse.builder()
                        .id(e.getId())
                        .actorName(e.getActor() != null ? e.getActor().getUsername() : "System")
                        .action(e.getAction())
                        .entityType(e.getEntityType())
                        .entityId(e.getEntityId())
                        .details(e.getDetails())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }
}
