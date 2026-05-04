package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.CreateTimeEntryRequest;
import com.stock_tracker.grey_toad.dto.TimeEntryResponse;
import com.stock_tracker.grey_toad.service.TimeEntryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks/{taskId}/time-entries")
public class TimeEntryController {

    private final TimeEntryService service;

    public TimeEntryController(TimeEntryService service) { this.service = service; }

    @PostMapping
    public TimeEntryResponse log(@PathVariable UUID taskId,
                                  @RequestBody @Valid CreateTimeEntryRequest request) {
        return service.log(taskId, request);
    }

    @GetMapping
    public List<TimeEntryResponse> getByTask(@PathVariable UUID taskId) {
        return service.getByTask(taskId);
    }

    @GetMapping("/total")
    public int getTotal(@PathVariable UUID taskId) {
        return service.getTotalMinutes(taskId);
    }
}
