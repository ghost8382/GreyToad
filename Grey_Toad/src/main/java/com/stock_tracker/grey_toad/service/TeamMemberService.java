package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.TeamRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.AddMemberRequest;
import com.stock_tracker.grey_toad.dto.TeamMemberResponse;
import com.stock_tracker.grey_toad.entity.Team;
import com.stock_tracker.grey_toad.entity.TeamMember;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public TeamMemberService(TeamMemberRepository teamMemberRepository,
                             UserRepository userRepository,
                             TeamRepository teamRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    public TeamMemberResponse addMember(UUID teamId, AddMemberRequest request) {

        // ✅ FIX: już nie parsujemy String → UUID
        UUID userId = request.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        // ✅ FIX: wydajne sprawdzenie zamiast findAll()
        if (teamMemberRepository.existsByUserIdAndTeamId(userId, teamId)) {
            throw new RuntimeException("User already in team");
        }

        TeamMember member = new TeamMember();
        member.setUser(user);
        member.setTeam(team);
        member.setRole(request.getRole());

        TeamMember saved = teamMemberRepository.save(member);

        return mapToResponse(saved);
    }

    public List<TeamMemberResponse> getMembers(UUID teamId) {

        // ✅ FIX: zamiast filtrowania w pamięci
        return teamMemberRepository.findByTeamId(teamId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void removeMember(UUID teamId, UUID memberId) {

        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (!member.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Member does not belong to this team");
        }

        teamMemberRepository.delete(member);
    }

    public void requireAdminOrLeader(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) throw new UnauthorizedException("Unauthorized");
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole()) && !"LEADER".equals(user.getRole())) {
            throw new ForbiddenException("Only administrators and leaders can manage team members");
        }
    }

    public void requireAdmin(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) throw new UnauthorizedException("Unauthorized");
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Only administrators can remove team members");
        }
    }

    private TeamMemberResponse mapToResponse(TeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .teamId(member.getTeam().getId())
                .role(member.getRole())
                .build();
    }
}