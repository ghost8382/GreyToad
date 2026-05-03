package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.RoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleTemplateRepository extends JpaRepository<RoleTemplate, UUID> {
    boolean existsByName(String name);
}
