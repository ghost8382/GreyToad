package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.DirectMessageResponse;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.DirectMessageService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/direct-messages")
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    public DirectMessageController(DirectMessageService directMessageService) {
        this.directMessageService = directMessageService;
    }

    @GetMapping("/{otherUserId}")
    public List<DirectMessageResponse> getConversation(@PathVariable UUID otherUserId,
                                                       Principal principal) {
        return directMessageService.getConversation(requirePrincipal(principal), otherUserId);
    }

    @GetMapping("/unread-counts")
    public Map<UUID, Long> getUnreadCounts(Principal principal) {
        return directMessageService.getUnreadCounts(requirePrincipal(principal));
    }

    @PatchMapping("/{otherUserId}/mark-read")
    public void markConversationRead(@PathVariable UUID otherUserId, Principal principal) {
        directMessageService.markConversationRead(requirePrincipal(principal), otherUserId);
    }

    private String requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized user");
        }
        return principal.getName();
    }
}
