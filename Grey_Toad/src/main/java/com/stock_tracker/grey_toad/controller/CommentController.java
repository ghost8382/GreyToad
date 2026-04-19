package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.CommentResponse;
import com.stock_tracker.grey_toad.dto.CreateCommentRequest;
import com.stock_tracker.grey_toad.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public CommentResponse create(
            @PathVariable UUID taskId,
            @RequestBody @Valid CreateCommentRequest request
    ) {
        return commentService.create(taskId, request);
    }

    @GetMapping
    public List<CommentResponse> getByTask(@PathVariable UUID taskId) {
        return commentService.getByTask(taskId);
    }
}