package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.CreateTaskRequest;
import com.stock_tracker.grey_toad.dto.TaskResponse;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public TaskResponse create(@RequestBody @Valid CreateTaskRequest request, Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        return taskService.create(request, principal.getName());
    }

    @GetMapping("/project/{projectId}")
    public List<TaskResponse> getByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "false") boolean showArchived) {
        return taskService.getByProject(projectId, showArchived);
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse changeStatus(
            @PathVariable UUID taskId,
            @RequestParam String status) {
        return taskService.changeStatus(taskId, status);
    }

    @PatchMapping("/{taskId}/assign")
    public TaskResponse assign(
            @PathVariable UUID taskId,
            @RequestParam UUID userId) {
        return taskService.assign(taskId, userId);
    }

    @PatchMapping("/{taskId}/archive")
    public TaskResponse archive(@PathVariable UUID taskId) {
        return taskService.archive(taskId);
    }

    @PatchMapping("/{taskId}/sla")
    public TaskResponse setSla(
            @PathVariable UUID taskId,
            @RequestParam String slaDeadline,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        return taskService.setSla(taskId, LocalDateTime.parse(slaDeadline), principal.getName());
    }

    @PatchMapping("/{taskId}/priority")
    public TaskResponse setPriority(
            @PathVariable UUID taskId,
            @RequestParam String priority,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        return taskService.setPriority(taskId, priority, principal.getName());
    }

    @PatchMapping("/{taskId}/type")
    public TaskResponse setType(
            @PathVariable UUID taskId,
            @RequestParam String type,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        return taskService.setType(taskId, type, principal.getName());
    }

    @PatchMapping("/{taskId}/deadline")
    public TaskResponse setDeadline(
            @PathVariable UUID taskId,
            @RequestParam String deadline,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized");
        return taskService.setDeadline(taskId, LocalDateTime.parse(deadline), principal.getName());
    }
}
