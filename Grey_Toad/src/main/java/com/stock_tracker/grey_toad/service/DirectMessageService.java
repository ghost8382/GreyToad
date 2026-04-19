package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.DirectMessageRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.DirectMessageResponse;
import com.stock_tracker.grey_toad.entity.DirectMessage;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.BadRequestException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DirectMessageService {

    private final DirectMessageRepository repository;
    private final UserRepository userRepository;

    public DirectMessageService(DirectMessageRepository repository,
                                UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public DirectMessageResponse send(String content,
                                      UUID senderId,
                                      UUID receiverId) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException("Sender not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new NotFoundException("Receiver not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("Cannot send direct message to yourself");
        }

        DirectMessage msg = new DirectMessage();
        msg.setContent(content);
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setCreatedAt(LocalDateTime.now());

        DirectMessage saved = repository.save(msg);

        return DirectMessageResponse.builder()
                .id(saved.getId())
                .content(saved.getContent())
                .senderId(sender.getId())
                .receiverId(receiver.getId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<DirectMessageResponse> getConversation(String requesterEmail, UUID otherUserId) {
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return repository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                        requester.getId(),
                        otherUser.getId(),
                        otherUser.getId(),
                        requester.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private DirectMessageResponse mapToResponse(DirectMessage message) {
        return DirectMessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
