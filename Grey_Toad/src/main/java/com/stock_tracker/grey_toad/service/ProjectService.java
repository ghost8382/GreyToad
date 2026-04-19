package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.ProjectRepository;
import com.stock_tracker.grey_toad.data.TeamRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.CreateProjectRequest;
import com.stock_tracker.grey_toad.dto.ProjectResponse;
import com.stock_tracker.grey_toad.entity.Project;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ChannelRepository channelRepository;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          TeamRepository teamRepository,
                          ChannelRepository channelRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.channelRepository = channelRepository;
    }

    public ProjectResponse create(CreateProjectRequest request, String userEmail) {
        requireAdmin(userEmail);

        Project project = new Project();
        project.setName(request.getName());

        return map(projectRepository.save(project));
    }

    public List<ProjectResponse> getAll(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<Project> projects = "ADMIN".equals(user.getRole())
                ? projectRepository.findAll()
                : projectRepository.findByMemberEmail(userEmail);
        return projects.stream().map(this::map).toList();
    }

    public ProjectResponse getById(UUID id, String userEmail) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole()) &&
                !projectRepository.existsMemberByEmailAndProjectId(userEmail, id)) {
            throw new ForbiddenException("Access denied");
        }
        return map(project);
    }

    @Transactional
    public void delete(UUID id, String userEmail) {
        requireAdmin(userEmail);
        channelRepository.softDeleteByProjectId(id);
        teamRepository.softDeleteByProjectId(id);
        projectRepository.softDeleteById(id);
    }

    private void requireAdmin(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Only administrators can perform this action");
        }
    }

    private ProjectResponse map(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .build();
    }
}
