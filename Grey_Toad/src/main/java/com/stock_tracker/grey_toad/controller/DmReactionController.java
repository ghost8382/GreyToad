package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.MessageReactionResponse;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.DmReactionService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/direct-messages/{dmId}/reactions")
public class DmReactionController {

    private final DmReactionService dmReactionService;

    public DmReactionController(DmReactionService dmReactionService) {
        this.dmReactionService = dmReactionService;
    }

    @PostMapping
    public List<MessageReactionResponse> toggle(
            @PathVariable UUID dmId,
            @RequestParam String emoji,
            Principal principal
    ) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        return dmReactionService.toggle(dmId, principal.getName(), emoji);
    }
}
