package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.AppNotificationResponse;
import com.stock_tracker.grey_toad.entity.AppNotification;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.AppNotificationService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class AppNotificationController {

    private final AppNotificationService service;
    private final UserRepository userRepository;

    public AppNotificationController(AppNotificationService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping("/mine")
    public List<AppNotificationResponse> getMine(Principal principal) {
        User user = requireUser(principal);
        return service.getUnread(user.getId()).stream().map(this::map).toList();
    }

    @PatchMapping("/read-all")
    @Transactional
    public void markAllRead(Principal principal) {
        User user = requireUser(principal);
        service.markAllRead(user.getId());
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public void markOneRead(@PathVariable UUID id, Principal principal) {
        requireUser(principal);
        service.markOneRead(id);
    }

    private AppNotificationResponse map(AppNotification n) {
        return AppNotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .projectId(n.getProjectId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private User requireUser(Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
