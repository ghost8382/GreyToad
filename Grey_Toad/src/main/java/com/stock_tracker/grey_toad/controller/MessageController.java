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
    public List<MessageResponse> getMessages(@PathVariable UUID channelId, Principal principal) {
        return messageService.getByChannel(channelId, principal != null ? principal.getName() : null);
    }

    @PostMapping("/{messageId}/replies")
    public MessageResponse reply(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @RequestBody @Valid CreateMessageRequest request,
            Principal principal) {
        return messageService.send(channelId, requirePrincipal(principal), request.getContent(), messageId);
    }

    @GetMapping("/{messageId}/replies")
    public List<MessageResponse> getReplies(@PathVariable UUID channelId,
                                             @PathVariable UUID messageId,
                                             Principal principal) {
        return messageService.getReplies(messageId, principal != null ? principal.getName() : null);
    }

    @GetMapping("/threads")
    public List<MessageResponse> getThreadStarters(@PathVariable UUID channelId) {
        return messageService.getThreadStarters(channelId);
    }

    @GetMapping("/posts")
    public List<MessageResponse> getPosts(@PathVariable UUID channelId, Principal principal) {
        return messageService.getPosts(channelId, principal != null ? principal.getName() : null);
    }

    @PostMapping("/posts")
    public MessageResponse createPost(@PathVariable UUID channelId,
                                       @RequestBody @Valid CreateMessageRequest request,
                                       Principal principal) {
        return messageService.createPost(channelId, requirePrincipal(principal), request.getContent());
    }

    private String requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized user");
        }

        return principal.getName();
    }
}
