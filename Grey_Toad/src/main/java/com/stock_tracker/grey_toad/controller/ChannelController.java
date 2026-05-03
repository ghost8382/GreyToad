package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.service.ChannelService;
import com.stock_tracker.grey_toad.dto.ChannelResponse;
import com.stock_tracker.grey_toad.dto.CreateChannelRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping
    public ChannelResponse create(@RequestBody @Valid CreateChannelRequest request) {
        return channelService.create(request);
    }

    @GetMapping("/team/{teamId}")
    public List<ChannelResponse> getByTeam(@PathVariable UUID teamId, Principal principal) {
        return channelService.getByTeam(teamId, principal != null ? principal.getName() : null);
    }

    @GetMapping("/project/{projectId}")
    public List<ChannelResponse> getByProject(@PathVariable UUID projectId) {
        return channelService.getByProject(projectId);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> deleteChannel(@PathVariable UUID channelId) {
        channelService.delete(channelId);
        return ResponseEntity.noContent().build();
    }
}