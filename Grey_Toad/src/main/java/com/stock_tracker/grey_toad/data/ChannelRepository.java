package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findByTeamId(UUID teamId);

    @Modifying
    @Query("UPDATE Channel c SET c.deleted = true WHERE c.team.id = :teamId")
    void softDeleteByTeamId(@Param("teamId") UUID teamId);

    @Modifying
    @Query("UPDATE Channel c SET c.deleted = true WHERE c.team.id IN (SELECT t.id FROM Team t WHERE t.project.id = :projectId)")
    void softDeleteByProjectId(@Param("projectId") UUID projectId);
}
