package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ProjectRepository;
import com.stock_tracker.grey_toad.data.TaskRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.TeamRepository;
import com.stock_tracker.grey_toad.data.TimeEntryRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.entity.TimeEntry;
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

import java.time.Duration;
import java.time.LocalDate;
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
    private final TimeEntryRepository timeEntryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditLogService auditLogService;
    private final AppNotificationService appNotificationService;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       TeamMemberRepository teamMemberRepository,
                       TeamRepository teamRepository,
                       TimeEntryRepository timeEntryRepository,
                       SimpMessagingTemplate messagingTemplate,
                       AuditLogService auditLogService,
                       AppNotificationService appNotificationService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.messagingTemplate = messagingTemplate;
        this.auditLogService = auditLogService;
        this.appNotificationService = appNotificationService;
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
            User assigned = findLeastLoadedMember(project);
            task.setAssignee(assigned);
            if (assigned != null) task.setAcceptanceStatus("PENDING");
        } else if (request.getAssigneeId() != null) {
            User user = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new NotFoundException("User not found"));
            task.setAssignee(user);
            task.setAcceptanceStatus("PENDING");
        }

        Task saved = taskRepository.save(task);

        if (saved.getAssignee() != null) {
            sendTaskNotification(saved);
        }

        auditLogService.log(userEmail, project.getId(), "TASK_CREATED", "TASK",
                saved.getId().toString(), "Created task #" + saved.getCaseNumber() + ": " + saved.getTitle());

        return broadcastAndReturn(saved);
    }

    public TaskResponse getById(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        return mapToResponse(task);
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
        String old = task.getStatus();
        task.setStatus(status);
        if ("DONE".equals(status)) {
            closeOpenTimeEntry(task.getId());
        }
        Task saved = taskRepository.save(task);
        auditLogService.log(null, task.getProject().getId(), "STATUS_CHANGED", "TASK",
                taskId.toString(), "#" + task.getCaseNumber() + " status: " + old + " → " + status);
        return broadcastAndReturn(saved);
    }

    public TaskResponse accept(UUID taskId, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the assignee can accept this task");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setAcceptanceStatus("ACCEPTED");
        task.setStatus("IN_PROGRESS");
        task.setWorkStartedAt(now);

        TimeEntry entry = new TimeEntry();
        entry.setTask(task);
        entry.setUser(user);
        entry.setStartedAt(now);
        entry.setDate(LocalDate.now());
        timeEntryRepository.save(entry);

        auditLogService.log(userEmail, task.getProject().getId(), "TASK_ACCEPTED", "TASK",
                taskId.toString(), "#" + task.getCaseNumber() + " accepted by " + user.getUsername());
        return broadcastAndReturn(taskRepository.save(task));
    }

    public TaskResponse reject(UUID taskId, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the assignee can reject this task");
        }
        closeOpenTimeEntry(task.getId());
        task.setAssignee(null);
        task.setAcceptanceStatus(null);
        task.setWorkStartedAt(null);
        task.setStatus("TODO");

        auditLogService.log(userEmail, task.getProject().getId(), "TASK_REJECTED", "TASK",
                taskId.toString(), "#" + task.getCaseNumber() + " rejected by " + user.getUsername());
        return broadcastAndReturn(taskRepository.save(task));
    }

    public TaskResponse assign(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        closeOpenTimeEntry(task.getId());
        task.setAssignee(user);
        task.setAcceptanceStatus("PENDING");
        task.setWorkStartedAt(null);
        Task saved = taskRepository.save(task);
        sendTaskNotification(saved);
        auditLogService.log(null, task.getProject().getId(), "ASSIGNED", "TASK",
                taskId.toString(), "#" + task.getCaseNumber() + " assigned to " + user.getUsername());
        return broadcastAndReturn(saved);
    }

    public TaskResponse archive(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setArchived(!task.isArchived());
        return broadcastAndReturn(taskRepository.save(task));
    }

    public TaskResponse setSla(UUID taskId, LocalDateTime slaDeadline, String userEmail) {
        requireAdmin(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setSlaDeadline(slaDeadline);
        return broadcastAndReturn(taskRepository.save(task));
    }

    public TaskResponse setPriority(UUID taskId, String priority, String userEmail) {
        requireAdminOrLeader(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setPriority(priority);
        auditLogService.log(userEmail, task.getProject().getId(), "PRIORITY_CHANGED", "TASK",
                taskId.toString(), "#" + task.getCaseNumber() + " priority set to " + priority);
        return broadcastAndReturn(taskRepository.save(task));
    }

    public TaskResponse setType(UUID taskId, String type, String userEmail) {
        requireAdminOrLeader(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setType(type);
        return broadcastAndReturn(taskRepository.save(task));
    }

    public TaskResponse setDeadline(UUID taskId, LocalDateTime deadline, String userEmail) {
        requireAdminOrLeader(userEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setDeadline(deadline);
        return broadcastAndReturn(taskRepository.save(task));
    }

    private void closeOpenTimeEntry(UUID taskId) {
        timeEntryRepository.findFirstByTaskIdAndEndedAtIsNull(taskId).ifPresent(entry -> {
            LocalDateTime now = LocalDateTime.now();
            entry.setEndedAt(now);
            if (entry.getStartedAt() != null) {
                entry.setMinutes((int) Duration.between(entry.getStartedAt(), now).toMinutes());
            }
            timeEntryRepository.save(entry);
        });
    }

    private TaskResponse broadcastAndReturn(Task task) {
        TaskResponse response = mapToResponse(task);
        messagingTemplate.convertAndSend(
                "/topic/tasks/" + task.getProject().getId(), response);
        return response;
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
        String projectId = task.getProject() != null ? task.getProject().getId().toString() : null;
        AppNotificationDto notification = AppNotificationDto.builder()
                .type("TASK_ASSIGNED")
                .title("You have been assigned a task")
                .body("#" + task.getCaseNumber() + " " + task.getTitle())
                .projectId(projectId)
                .build();
        messagingTemplate.convertAndSendToUser(
                task.getAssignee().getEmail(),
                "/queue/notifications",
                notification
        );
        // Only persist for offline users — online users receive via WS
        if (!task.getAssignee().isOnline()) {
            appNotificationService.persist(
                    task.getAssignee().getId(),
                    "TASK_ASSIGNED",
                    notification.getTitle(),
                    notification.getBody(),
                    projectId
            );
        }
    }

    private TaskResponse mapToResponse(Task task) {
        List<String> teamNames = teamRepository.findByProjectId(task.getProject().getId())
                .stream().map(Team::getName).toList();
        return mapToResponse(task, teamNames);
    }

    private TaskResponse mapToResponse(Task task, List<String> teamNames) {
        int closedMinutes = timeEntryRepository.sumMinutesByTaskId(task.getId());
        var activeSession = timeEntryRepository.findFirstByTaskIdAndEndedAtIsNull(task.getId());
        int liveMinutes = activeSession
                .filter(e -> e.getStartedAt() != null)
                .map(e -> (int) Duration.between(e.getStartedAt(), LocalDateTime.now()).toMinutes())
                .orElse(0);
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
                .acceptanceStatus(task.getAcceptanceStatus())
                .totalWorkedMinutes(closedMinutes + liveMinutes)
                .workingSessionActive(activeSession.isPresent())
                .workStartedAt(task.getWorkStartedAt())
                .build();
    }
}
