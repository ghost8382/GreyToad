package com.stock_tracker.grey_toad.data;

import com.stock_tracker.grey_toad.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    boolean existsByUserIdAndTeamId(UUID userId, UUID teamId);

    List<TeamMember> findByTeamId(UUID teamId);

    List<TeamMember> findByUserId(UUID userId);

    void deleteByTeamId(UUID teamId);
}
