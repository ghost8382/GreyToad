package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.entity.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PresenceService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void resetOnStartup() {
        userRepository.markAllOffline();
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void evictInactive() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<User> inactive = userRepository.findByOnlineTrueAndLastSeenBefore(threshold);
        for (User u : inactive) {
            u.setOnline(false);
            userRepository.save(u);
            broadcast(u.getId().toString(), false);
        }
    }

    @Transactional
    public void markOnline(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            boolean wasOffline = !u.isOnline();
            u.setOnline(true);
            u.setLastSeen(LocalDateTime.now());
            userRepository.save(u);
            if (wasOffline) broadcast(u.getId().toString(), true);
        });
    }

    @Transactional
    public void markOffline(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            if (u.isOnline()) {
                u.setOnline(false);
                u.setLastSeen(LocalDateTime.now());
                userRepository.save(u);
                broadcast(u.getId().toString(), false);
            }
        });
    }

    @Transactional
    public void heartbeat(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            boolean wasOffline = !u.isOnline();
            u.setOnline(true);
            u.setLastSeen(LocalDateTime.now());
            userRepository.save(u);
            if (wasOffline) broadcast(u.getId().toString(), true);
        });
    }

    private void broadcast(String userId, boolean isOnline) {
        messagingTemplate.convertAndSend("/topic/presence",
                Map.of("userId", userId, "isOnline", isOnline));
    }
}
