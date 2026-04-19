package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.CreateTeamRequest;
import com.stock_tracker.grey_toad.dto.TeamResponse;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public TeamResponse create(@RequestBody @Valid CreateTeamRequest request,
                               Principal principal) {
        return teamService.create(request, requirePrincipal(principal));
    }

    @GetMapping
    public List<TeamResponse> getAll() {
        return teamService.getAll();
    }

    @GetMapping("/my")
    public List<TeamResponse> getMyTeams(Principal principal) {
        return teamService.getMyTeams(requirePrincipal(principal));
    }

    @GetMapping("/project/{projectId}")
    public List<TeamResponse> getByProject(@PathVariable UUID projectId) {
        return teamService.getByProject(projectId);
    }

    @GetMapping("/{id}")
    public TeamResponse getById(@PathVariable UUID id) {
        return teamService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id, Principal principal) {
        teamService.delete(id, requirePrincipal(principal));
    }

    private String requirePrincipal(Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized user");
        return principal.getName();
    }
}
