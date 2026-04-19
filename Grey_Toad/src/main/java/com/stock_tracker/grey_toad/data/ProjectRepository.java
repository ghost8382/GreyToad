package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.Project;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("SELECT DISTINCT tm.team.project FROM TeamMember tm WHERE tm.user.email = :email")
    List<Project> findByMemberEmail(@Param("email") String email);

    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.user.email = :email AND tm.team.project.id = :projectId")
    boolean existsMemberByEmailAndProjectId(@Param("email") String email, @Param("projectId") UUID projectId);

    @Modifying
    @Query("UPDATE Project p SET p.deleted = true WHERE p.id = :id")
    void softDeleteById(@Param("id") UUID id);
}