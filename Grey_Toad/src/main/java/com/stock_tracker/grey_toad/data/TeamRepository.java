package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findByProjectId(UUID projectId);

    @Query(nativeQuery = true, value =
        "SELECT t.* FROM teams t WHERE (t.deleted IS NOT TRUE) AND " +
        "(t.project_id IS NULL OR EXISTS (" +
        "  SELECT 1 FROM projects p WHERE p.id = t.project_id AND (p.deleted IS NOT TRUE)" +
        "))")
    List<Team> findAllActive();

    @Modifying
    @Query("UPDATE Team t SET t.deleted = true WHERE t.project.id = :projectId")
    void softDeleteByProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Query("UPDATE Team t SET t.deleted = true WHERE t.id = :id")
    void softDeleteById(@Param("id") UUID id);
}
