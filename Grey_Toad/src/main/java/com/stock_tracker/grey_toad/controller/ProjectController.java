package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.CreateProjectRequest;
import com.stock_tracker.grey_toad.dto.ProjectResponse;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectResponse create(@RequestBody @Valid CreateProjectRequest request,
                                  Principal principal) {
        return projectService.create(request, requirePrincipal(principal));
    }

    @GetMapping
    public List<ProjectResponse> getAll(Principal principal) {
        return projectService.getAll(requirePrincipal(principal));
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable UUID id, Principal principal) {
        return projectService.getById(id, requirePrincipal(principal));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id, Principal principal) {
        projectService.delete(id, requirePrincipal(principal));
    }

    private String requirePrincipal(Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized user");
        return principal.getName();
    }
}
