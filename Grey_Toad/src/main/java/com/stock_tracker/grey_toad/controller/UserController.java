package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.UpdateProfileRequest;
import com.stock_tracker.grey_toad.dto.UserResponse;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.service.PresenceService;
import com.stock_tracker.grey_toad.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final PresenceService presenceService;

    public UserController(UserService userService, PresenceService presenceService) {
        this.userService = userService;
        this.presenceService = presenceService;
    }

    @PostMapping
    public UserResponse create(@RequestBody User user, Principal principal) {
        if (principal == null) throw new RuntimeException("Unauthorized");
        return userService.createByAdmin(user, principal.getName());
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @RequestBody User user) {
        return userService.update(id, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getMe(authentication.getName());
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(Authentication authentication,
                                      @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }

    @PatchMapping("/{id}/role")
    public UserResponse setRole(@PathVariable UUID id,
                                @RequestParam String role,
                                Principal principal) {
        if (principal == null) throw new RuntimeException("Unauthorized");
        return userService.setRole(id, role, principal.getName());
    }

    @PatchMapping("/{id}/job-title")
    public UserResponse setJobTitle(@PathVariable UUID id,
                                    @RequestParam(required = false) String jobTitle,
                                    Principal principal) {
        if (principal == null) throw new RuntimeException("Unauthorized");
        return userService.setJobTitle(id, jobTitle, principal.getName());
    }

    @PutMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(Authentication authentication) {
        presenceService.heartbeat(authentication.getName());
        return ResponseEntity.ok().build();
    }
}