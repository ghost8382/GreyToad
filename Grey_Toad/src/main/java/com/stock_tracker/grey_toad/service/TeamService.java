package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.ProjectRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.TeamRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.CreateTeamRequest;
import com.stock_tracker.grey_toad.dto.TeamResponse;
import com.stock_tracker.grey_toad.entity.Project;
import com.stock_tracker.grey_toad.entity.Team;
import com.stock_tracker.grey_toad.entity.TeamMember;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final ChannelRepository channelRepository;

    public TeamService(TeamRepository teamRepository,
                       UserRepository userRepository,
                       TeamMemberRepository teamMemberRepository,
                       ProjectRepository projectRepository,
                       ChannelRepository channelRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.projectRepository = projectRepository;
        this.channelRepository = channelRepository;
    }

    public TeamResponse create(CreateTeamRequest request, String ownerEmail) {
        User owner = requireAdminOrLeader(ownerEmail);

        Team team = new Team();
        team.setName(request.getName());
        team.setOwner(owner);

        if (request.getProjectId() != null && !request.getProjectId().isBlank()) {
            UUID projectId = UUID.fromString(request.getProjectId());
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new NotFoundException("Project not found"));
            team.setProject(project);
        }

        Team savedTeam = teamRepository.save(team);

        TeamMember member = new TeamMember();
        member.setUser(owner);
        member.setTeam(savedTeam);
        member.setRole("OWNER");
        teamMemberRepository.save(member);

        return mapToResponse(savedTeam);
    }

    public List<TeamResponse> getAll() {
        return teamRepository.findAllActive()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TeamResponse> getByProject(UUID projectId) {
        return teamRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TeamResponse getById(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Team not found"));
        return mapToResponse(team);
    }

    public List<TeamResponse> getMyTeams(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return teamMemberRepository.findByUserId(user.getId())
                .stream()
                .map(m -> mapToResponse(m.getTeam()))
                .toList();
    }

    @Transactional
    public void delete(UUID id, String userEmail) {
        requireAdmin(userEmail);
        channelRepository.softDeleteByTeamId(id);
        teamMemberRepository.deleteByTeamId(id);
        teamRepository.softDeleteById(id);
    }

    private User requireAdmin(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Only administrators can perform this action");
        }
        return user;
    }

    private User requireAdminOrLeader(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole()) && !"LEADER".equals(user.getRole())) {
            throw new ForbiddenException("Only administrators and leaders can perform this action");
        }
        return user;
    }

    private TeamResponse mapToResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .ownerId(team.getOwner() != null ? team.getOwner().getId() : null)
                .projectId(team.getProject() != null ? team.getProject().getId() : null)
                .build();
    }
}
