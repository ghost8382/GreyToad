package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.AttachmentResponse;
import com.stock_tracker.grey_toad.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
public class AttachmentController {

    private final AttachmentService service;

    public AttachmentController(AttachmentService service) { this.service = service; }

    @PostMapping("/tasks/{taskId}/attachments")
    public AttachmentResponse uploadToTask(@PathVariable UUID taskId,
                                            @RequestParam("file") MultipartFile file,
                                            Principal principal) throws IOException {
        return service.uploadToTask(taskId, principal.getName(), file);
    }

    @GetMapping("/tasks/{taskId}/attachments")
    public List<AttachmentResponse> getTaskAttachments(@PathVariable UUID taskId) {
        return service.getByTask(taskId);
    }

    @PostMapping("/messages/{messageId}/attachments")
    public AttachmentResponse uploadToMessage(@PathVariable UUID messageId,
                                               @RequestParam("file") MultipartFile file,
                                               Principal principal) throws IOException {
        return service.uploadToMessage(messageId, principal.getName(), file);
    }

    @GetMapping("/messages/{messageId}/attachments")
    public List<AttachmentResponse> getMessageAttachments(@PathVariable UUID messageId) {
        return service.getByMessage(messageId);
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws IOException {
        Resource resource = service.download(id);
        String contentType = service.getContentType(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
