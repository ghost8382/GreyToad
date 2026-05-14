package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    boolean existsByUserIdAndTeamId(UUID userId, UUID teamId);

    List<TeamMember> findByTeamId(UUID teamId);

    @Query("SELECT m.team.id FROM TeamMember m WHERE m.user.id = :userId")
    List<UUID> findTeamIdsByUserId(@Param("userId") UUID userId);

    void deleteByTeamId(UUID teamId);

    void deleteByUserId(UUID userId);
}
