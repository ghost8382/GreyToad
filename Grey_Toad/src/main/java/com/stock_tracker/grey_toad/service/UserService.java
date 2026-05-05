package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.UpdateProfileRequest;
import com.stock_tracker.grey_toad.dto.UserResponse;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    public UserResponse createByAdmin(User user, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(requester.getRole())) {
            throw new com.stock_tracker.grey_toad.exceptions.ForbiddenException("Only administrators can create accounts");
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        return create(user);
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponse update(UUID id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (target.isHeadAdmin()) {
            throw new com.stock_tracker.grey_toad.exceptions.ForbiddenException("Cannot delete the Head Admin account");
        }
        userRepository.softDeleteById(id);
    }

    public UserResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return mapToResponse(user);
    }

    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return mapToResponse(user);
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (request.getQuote() != null) {
            user.setQuote(request.getQuote());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(request.getStatus());
        }
        return mapToResponse(userRepository.save(user));
    }

    public UserResponse setRole(UUID userId, String role, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(requester.getRole())) {
            throw new com.stock_tracker.grey_toad.exceptions.ForbiddenException("Only administrators can change roles");
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (target.isHeadAdmin()) {
            throw new com.stock_tracker.grey_toad.exceptions.ForbiddenException("Cannot change the role of the Head Admin");
        }
        target.setRole(role);
        return mapToResponse(userRepository.save(target));
    }

    public UserResponse setJobTitle(UUID userId, String jobTitle, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!"ADMIN".equals(requester.getRole())) {
            throw new com.stock_tracker.grey_toad.exceptions.ForbiddenException("Only administrators can change job titles");
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        target.setJobTitle(jobTitle == null || jobTitle.isBlank() ? null : jobTitle.trim());
        return mapToResponse(userRepository.save(target));
    }

    public User getEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .jobTitle(user.getJobTitle())
                .quote(user.getQuote())
                .status(user.getStatus())
                .isOnline(user.isOnline())
                .lastSeen(user.getLastSeen())
                .headAdmin(user.isHeadAdmin())
                .build();
    }
}