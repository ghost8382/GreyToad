package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.AddMemberRequest;
import com.stock_tracker.grey_toad.dto.TeamMemberResponse;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.TeamMemberService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/teams/{teamId}/members")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    public TeamMemberController(TeamMemberService teamMemberService) {
        this.teamMemberService = teamMemberService;
    }

    @PostMapping
    public TeamMemberResponse addMember(
            @PathVariable UUID teamId,
            @RequestBody @Valid AddMemberRequest request,
            Principal principal
    ) {
        requireAdminOrLeader(principal);
        return teamMemberService.addMember(teamId, request);
    }

    @GetMapping
    public List<TeamMemberResponse> getMembers(@PathVariable UUID teamId) {
        return teamMemberService.getMembers(teamId);
    }

    @DeleteMapping("/{memberId}")
    public void removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID memberId,
            Principal principal
    ) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        teamMemberService.requireAdmin(principal.getName());
        teamMemberService.removeMember(teamId, memberId);
    }

    private void requireAdminOrLeader(Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        teamMemberService.requireAdminOrLeader(principal.getName());
    }
}
