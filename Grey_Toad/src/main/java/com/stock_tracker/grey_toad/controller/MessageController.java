package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.service.MessageService;
import com.stock_tracker.grey_toad.dto.CreateMessageRequest;
import com.stock_tracker.grey_toad.dto.MessageResponse;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/channels/{channelId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public MessageResponse send(
            @PathVariable UUID channelId,
            @RequestBody @Valid CreateMessageRequest request,
            Principal principal
    ) {
        return messageService.send(channelId, requirePrincipal(principal), request.getContent());
    }

    @GetMapping
    public List<MessageResponse> getMessages(@PathVariable UUID channelId) {
        return messageService.getByChannel(channelId);
    }

    private String requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized user");
        }

        return principal.getName();
    }
}
