package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.AttachmentRepository;
import com.stock_tracker.grey_toad.data.MessageRepository;
import com.stock_tracker.grey_toad.data.TaskRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.AttachmentResponse;
import com.stock_tracker.grey_toad.entity.Attachment;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             TaskRepository taskRepository,
                             MessageRepository messageRepository,
                             UserRepository userRepository) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public AttachmentResponse uploadToTask(UUID taskId, String uploaderEmail, MultipartFile file) throws IOException {
        var task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        var uploader = userRepository.findByEmail(uploaderEmail).orElseThrow(() -> new NotFoundException("User not found"));
        String stored = save(file);
        Attachment a = new Attachment();
        a.setTask(task); a.setUploader(uploader);
        a.setOriginalName(file.getOriginalFilename()); a.setStoredName(stored);
        a.setContentType(file.getContentType()); a.setFileSize(file.getSize());
        return mapToResponse(attachmentRepository.save(a));
    }

    public AttachmentResponse uploadToMessage(UUID messageId, String uploaderEmail, MultipartFile file) throws IOException {
        var message = messageRepository.findById(messageId).orElseThrow(() -> new NotFoundException("Message not found"));
        var uploader = userRepository.findByEmail(uploaderEmail).orElseThrow(() -> new NotFoundException("User not found"));
        String stored = save(file);
        Attachment a = new Attachment();
        a.setMessage(message); a.setUploader(uploader);
        a.setOriginalName(file.getOriginalFilename()); a.setStoredName(stored);
        a.setContentType(file.getContentType()); a.setFileSize(file.getSize());
        return mapToResponse(attachmentRepository.save(a));
    }

    public List<AttachmentResponse> getByTask(UUID taskId) {
        return attachmentRepository.findByTaskId(taskId).stream().map(this::mapToResponse).toList();
    }

    public List<AttachmentResponse> getByMessage(UUID messageId) {
        return attachmentRepository.findByMessageId(messageId).stream().map(this::mapToResponse).toList();
    }

    public Resource download(UUID id) throws IOException {
        Attachment a = attachmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Attachment not found"));
        Path path = Paths.get(uploadDir).resolve(a.getStoredName()).normalize();
        return new UrlResource(path.toUri());
    }

    public String getContentType(UUID id) {
        return attachmentRepository.findById(id).map(Attachment::getContentType).orElse("application/octet-stream");
    }

    private String save(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String stored = UUID.randomUUID() + "_" + file.getOriginalFilename();
        file.transferTo(dir.resolve(stored).toFile());
        return stored;
    }

    private AttachmentResponse mapToResponse(Attachment a) {
        return AttachmentResponse.builder()
                .id(a.getId()).originalName(a.getOriginalName())
                .contentType(a.getContentType()).fileSize(a.getFileSize())
                .uploaderName(a.getUploader().getUsername()).createdAt(a.getCreatedAt())
                .build();
    }
}
