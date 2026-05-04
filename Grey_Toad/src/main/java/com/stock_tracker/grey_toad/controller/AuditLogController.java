package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.AuditLogResponse;
import com.stock_tracker.grey_toad.service.AuditLogService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/audit-log")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) { this.service = service; }

    @GetMapping("/project/{projectId}")
    public List<AuditLogResponse> getByProject(@PathVariable UUID projectId) {
        return service.getByProject(projectId);
    }
}
