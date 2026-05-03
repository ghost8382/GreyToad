package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.TaskRepository;
import com.stock_tracker.grey_toad.data.TimeEntryRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.CreateTimeEntryRequest;
import com.stock_tracker.grey_toad.dto.TimeEntryResponse;
import com.stock_tracker.grey_toad.entity.Task;
import com.stock_tracker.grey_toad.entity.TimeEntry;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TimeEntryService(TimeEntryRepository timeEntryRepository,
                            TaskRepository taskRepository,
                            UserRepository userRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public TimeEntryResponse log(UUID taskId, CreateTimeEntryRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        TimeEntry entry = new TimeEntry();
        entry.setTask(task);
        entry.setUser(user);
        entry.setMinutes(request.getMinutes());
        entry.setDescription(request.getDescription());
        entry.setDate(request.getDate());

        return mapToResponse(timeEntryRepository.save(entry));
    }

    public List<TimeEntryResponse> getByTask(UUID taskId) {
        return timeEntryRepository.findByTaskIdOrderByDateDesc(taskId)
                .stream().map(this::mapToResponse).toList();
    }

    public int getTotalMinutes(UUID taskId) {
        return timeEntryRepository.sumMinutesByTaskId(taskId);
    }

    private TimeEntryResponse mapToResponse(TimeEntry e) {
        return TimeEntryResponse.builder()
                .id(e.getId())
                .taskId(e.getTask().getId())
                .userId(e.getUser().getId())
                .userName(e.getUser().getUsername())
                .minutes(e.getMinutes())
                .description(e.getDescription())
                .date(e.getDate())
                .build();
    }
}
