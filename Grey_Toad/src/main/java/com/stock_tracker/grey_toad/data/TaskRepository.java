package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectId(UUID projectId);

    List<Task> findByProjectIdAndArchivedFalse(UUID projectId);

    List<Task> findByAssigneeId(UUID assigneeId);

    long countByProjectId(UUID projectId);

    long countByAssigneeIdAndProjectId(UUID assigneeId, UUID projectId);

    java.util.Optional<Task> findByProjectIdAndCaseNumber(UUID projectId, Integer caseNumber);
}
