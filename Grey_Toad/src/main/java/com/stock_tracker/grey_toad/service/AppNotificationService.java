package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.AppNotificationRepository;
import com.stock_tracker.grey_toad.entity.AppNotification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AppNotificationService {

    private final AppNotificationRepository repo;

    public AppNotificationService(AppNotificationRepository repo) {
        this.repo = repo;
    }

    public AppNotification persist(UUID recipientId, String type, String title, String body, String projectId) {
        AppNotification n = new AppNotification();
        n.setRecipientId(recipientId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setProjectId(projectId);
        return repo.save(n);
    }

    public List<AppNotification> getUnread(UUID recipientId) {
        return repo.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId);
    }

    @Transactional
    public void markAllRead(UUID recipientId) {
        repo.markAllReadByRecipient(recipientId);
    }

    @Transactional
    public void markOneRead(UUID id) {
        repo.markOneRead(id);
    }
}
