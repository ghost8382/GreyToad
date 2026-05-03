package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.CreateRoleTemplateRequest;
import com.stock_tracker.grey_toad.dto.RoleTemplateResponse;
import com.stock_tracker.grey_toad.service.RoleTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/role-templates")
public class RoleTemplateController {

    private final RoleTemplateService service;

    public RoleTemplateController(RoleTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<RoleTemplateResponse> getAll() {
        return service.getAll();
    }

    @PostMapping
    public RoleTemplateResponse create(@RequestBody @Valid CreateRoleTemplateRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
