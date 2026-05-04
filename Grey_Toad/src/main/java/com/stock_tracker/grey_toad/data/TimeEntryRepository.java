package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {
    List<TimeEntry> findByTaskIdOrderByDateDesc(UUID taskId);
    java.util.Optional<TimeEntry> findByTaskIdAndEndedAtIsNull(UUID taskId);

    @Query("SELECT COALESCE(SUM(t.minutes), 0) FROM TimeEntry t WHERE t.task.id = :taskId")
    int sumMinutesByTaskId(UUID taskId);

    @Query("SELECT COALESCE(SUM(t.minutes), 0) FROM TimeEntry t WHERE t.task.project.id = :projectId")
    int sumMinutesByProjectId(UUID projectId);
}
