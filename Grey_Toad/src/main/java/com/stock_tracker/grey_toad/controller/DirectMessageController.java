package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.DirectMessageResponse;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.DirectMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
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

    private String requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized user");
        }

        return principal.getName();
    }
}
