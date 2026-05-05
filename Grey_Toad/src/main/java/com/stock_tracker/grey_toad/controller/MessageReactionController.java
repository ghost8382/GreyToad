package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.MessageReactionResponse;
import com.stock_tracker.grey_toad.service.MessageReactionService;
import com.stock_tracker.grey_toad.service.MessageService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/messages/{messageId}/reactions")
public class MessageReactionController {

    private final MessageReactionService service;
    private final MessageService messageService;

    public MessageReactionController(MessageReactionService service, MessageService messageService) {
        this.service = service;
        this.messageService = messageService;
    }

    @PostMapping
    public List<MessageReactionResponse> toggle(
            @PathVariable UUID messageId,
            @RequestParam String emoji,
            Principal principal) {
        List<MessageReactionResponse> result = service.toggle(messageId, principal.getName(), emoji);
        messageService.broadcastMessageUpdate(messageId);
        return result;
    }

    @GetMapping
    public List<MessageReactionResponse> get(@PathVariable UUID messageId, Principal principal) {
        return service.getForMessage(messageId, principal != null ? principal.getName() : null);
    }
}
