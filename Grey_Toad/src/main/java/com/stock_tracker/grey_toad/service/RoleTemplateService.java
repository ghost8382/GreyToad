package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.RoleTemplateRepository;
import com.stock_tracker.grey_toad.dto.CreateRoleTemplateRequest;
import com.stock_tracker.grey_toad.dto.RoleTemplateResponse;
import com.stock_tracker.grey_toad.entity.RoleTemplate;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoleTemplateService {

    private final RoleTemplateRepository repo;

    public RoleTemplateService(RoleTemplateRepository repo) {
        this.repo = repo;
    }

    public List<RoleTemplateResponse> getAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    public RoleTemplateResponse create(CreateRoleTemplateRequest request) {
        RoleTemplate t = new RoleTemplate();
        t.setName(request.getName().trim());
        t.setPermissionLevel(request.getPermissionLevel() != null ? request.getPermissionLevel() : "USER");
        return map(repo.save(t));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new NotFoundException("Role template not found");
        repo.deleteById(id);
    }

    private RoleTemplateResponse map(RoleTemplate t) {
        return RoleTemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .permissionLevel(t.getPermissionLevel())
                .build();
    }
}
