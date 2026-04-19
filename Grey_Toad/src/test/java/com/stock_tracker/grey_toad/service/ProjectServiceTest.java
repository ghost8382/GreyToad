package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ProjectRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.TeamRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.CreateProjectRequest;
import com.stock_tracker.grey_toad.entity.Team;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.BadRequestException;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createRejectsInvalidTeamId() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Project X");
        request.setTeamId("not-a-uuid");

        User user = new User();
        user.setEmail("dev@example.com");

        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> projectService.create(request, "dev@example.com"));
        verifyNoInteractions(teamRepository, teamMemberRepository, projectRepository);
    }

    @Test
    void createRejectsUserOutsideTeam() {
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Project X");
        request.setTeamId(teamId.toString());

        User user = new User();
        user.setId(userId);
        user.setEmail("dev@example.com");

        Team team = new Team();
        team.setId(teamId);

        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(user));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByUserIdAndTeamId(userId, teamId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> projectService.create(request, "dev@example.com"));
    }
}
