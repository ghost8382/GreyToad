package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.MessageReactionResponse;
import com.stock_tracker.grey_toad.service.MessageReactionService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/messages/{messageId}/reactions")
public class MessageReactionController {

    private final MessageReactionService service;

    public MessageReactionController(MessageReactionService service) { this.service = service; }

    @PostMapping
    public List<MessageReactionResponse> toggle(
            @PathVariable UUID messageId,
            @RequestParam String emoji,
            Principal principal) {
        return service.toggle(messageId, principal.getName(), emoji);
    }

    @GetMapping
    public List<MessageReactionResponse> get(@PathVariable UUID messageId, Principal principal) {
        return service.getForMessage(messageId, principal != null ? principal.getName() : null);
    }
}
