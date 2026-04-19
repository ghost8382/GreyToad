package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ProjectRepository;
import com.stock_tracker.grey_toad.data.TaskRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.TeamRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.CreateTaskRequest;
import com.stock_tracker.grey_toad.dto.AppNotificationDto;
import com.stock_tracker.grey_toad.dto.TaskResponse;
import com.stock_tracker.grey_toad.entity.Project;
import com.stock_tracker.grey_toad.entity.Task;
import com.stock_tracker.grey_toad.entity.Team;
import com.stock_tracker.grey_toad.entity.TeamMember;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       TeamMemberRepository teamMemberRepository,
                       TeamRepository teamRepository,
                       SimpMessagingTemplate messagingTemplate) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public TaskResponse create(CreateTaskRequest request, String userEmail) {
        requireAdminOrLeader(userEmail);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setProject(project);
        task.setStatus(request.getStatus() != null ? request.getStatus() : "TODO");
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setType(request.getType() != null ? request.getType() : "TASK");

        long count = taskRepository.countByProjectId(request.getProjectId());
        task.setCaseNumber((int) count + 1);

        if (request.isAutoAssign()) {
            task.setAssignee(findLeastLoadedMember(project));
        } else if (request.getAssigneeId() != null) {
            User user = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new NotFoundException("User not found"));
            task.setAssignee(user);
        }

        Task saved = taskRepository.save(task);

        if (saved.getAssignee() != null) {
            sendTaskNotification(saved);
        }

        return mapToResponse(saved);
    }

    public List<TaskResponse> getByProject(UUID projectId, boolean showArchived) {
        List<Task> tasks = showArchived
                ? taskRepository.findByProjectId(projectId)
                : taskRepository.findByProjectIdAndArchivedFalse(projectId);

        // Load teams once for this project to avoid N+1
        List<String> teamNames = teamRepository.findByProjectId(projectId)
                .stream().map(Team::getName).toList();

        return tasks.stream().map(t -> mapToResponse(t, teamNames)).toList();
    }

    public TaskResponse changeStatus(UUID taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setStatus(status);
        return mapToResponse(taskRepository.save(task));
    }

    public TaskResponse assign(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        task.setAssignee(user);
        Task saved = taskRepository.save(task);
        sendTaskNotification(saved);
        return mapToResponse(saved);
    }

    public TaskResponse archive(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setArchived(!task.isArchived());
        return mapToResponse(taskRepository.save(task));
    }

    public TaskResponse setSla(UUID taskId, LocalDateTime slaDeadline, String userEmail) {
        requireAdmin(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setSlaDeadline(slaDeadline);
        return mapToResponse(taskRepository.save(task));
    }

    public TaskResponse setPriority(UUID taskId, String priority, String userEmail) {
        requireAdminOrLeader(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setPriority(priority);
        return mapToResponse(taskRepository.save(task));
    }

    public TaskResponse setType(UUID taskId, String type, String userEmail) {
        requireAdminOrLeader(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setType(type);
        return mapToResponse(taskRepository.save(task));
    }

    public TaskResponse setDeadline(UUID taskId, LocalDateTime deadline, String userEmail) {
        requireAdminOrLeader(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setDeadline(deadline);
        return mapToResponse(taskRepository.save(task));
    }

    private void requireAdminOrLeader(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole()) && !"LEADER".equals(user.getRole())) {
            throw new ForbiddenException("Only administrators and leaders can perform this action");
        }
    }

    private void requireAdmin(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Only administrators can perform this action");
        }
    }

    private User findLeastLoadedMember(Project project) {
        List<Team> teams = teamRepository.findByProjectId(project.getId());
        if (teams.isEmpty()) return null;

        User best = null;
        long minLoad = Long.MAX_VALUE;
        for (Team team : teams) {
            for (TeamMember m : teamMemberRepository.findByTeamId(team.getId())) {
                long load = taskRepository.countByAssigneeIdAndProjectId(
                        m.getUser().getId(), project.getId());
                if (load < minLoad) {
                    minLoad = load;
                    best = m.getUser();
                }
            }
        }
        return best;
    }

    private void sendTaskNotification(Task task) {
        AppNotificationDto notification = AppNotificationDto.builder()
                .type("TASK_ASSIGNED")
                .title("Przypisano Ci zadanie")
                .body("#" + task.getCaseNumber() + " " + task.getTitle())
                .build();
        messagingTemplate.convertAndSendToUser(
                task.getAssignee().getEmail(),
                "/queue/notifications",
                notification
        );
    }

    private TaskResponse mapToResponse(Task task) {
        List<String> teamNames = teamRepository.findByProjectId(task.getProject().getId())
                .stream().map(Team::getName).toList();
        return mapToResponse(task, teamNames);
    }

    private TaskResponse mapToResponse(Task task, List<String> teamNames) {
        return TaskResponse.builder()
                .id(task.getId())
                .caseNumber(task.getCaseNumber())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getUsername() : null)
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .teamNames(teamNames)
                .deadline(task.getDeadline())
                .slaDeadline(task.getSlaDeadline())
                .archived(task.isArchived())
                .priority(task.getPriority())
                .type(task.getType())
                .build();
    }
}
