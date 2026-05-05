package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.DirectMessageRepository;
import com.stock_tracker.grey_toad.data.DmReactionRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.DirectMessageResponse;
import com.stock_tracker.grey_toad.dto.MessageReactionResponse;
import com.stock_tracker.grey_toad.entity.DirectMessage;
import com.stock_tracker.grey_toad.entity.DmReaction;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.BadRequestException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DirectMessageService {

    private final DirectMessageRepository repository;
    private final UserRepository userRepository;
    private final DmReactionRepository dmReactionRepository;

    public DirectMessageService(DirectMessageRepository repository,
                                UserRepository userRepository,
                                DmReactionRepository dmReactionRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.dmReactionRepository = dmReactionRepository;
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
        msg.setRead(false);

        DirectMessage saved = repository.save(msg);

        return mapToResponse(saved, List.of(), null);
    }

    public List<DirectMessageResponse> getConversation(String requesterEmail, UUID otherUserId) {
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<DirectMessage> messages = repository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                requester.getId(), otherUser.getId(), otherUser.getId(), requester.getId());

        Map<UUID, List<DmReaction>> reactionsMap = messages.isEmpty() ? Map.of() :
                dmReactionRepository.findByDirectMessageIdIn(messages.stream().map(DirectMessage::getId).toList())
                        .stream().collect(Collectors.groupingBy(r -> r.getDirectMessage().getId()));

        return messages.stream()
                .map(m -> mapToResponse(m, reactionsMap.getOrDefault(m.getId(), List.of()), requester.getId()))
                .toList();
    }

    public Map<UUID, Long> getUnreadCounts(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<Object[]> rows = repository.countUnreadGroupedBySender(requester.getId());
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], (Long) row[1]);
        }
        return result;
    }

    @Transactional
    public void markConversationRead(String requesterEmail, UUID otherUserId) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        repository.markConversationRead(otherUserId, requester.getId());
    }

    private DirectMessageResponse mapToResponse(DirectMessage message, List<DmReaction> reactions, UUID currentUserId) {
        Map<String, List<DmReaction>> byEmoji = reactions.stream()
                .collect(Collectors.groupingBy(DmReaction::getEmoji));

        List<MessageReactionResponse> reactionResponses = byEmoji.entrySet().stream()
                .map(e -> MessageReactionResponse.builder()
                        .emoji(e.getKey())
                        .count(e.getValue().size())
                        .reactedByMe(currentUserId != null &&
                                e.getValue().stream().anyMatch(r -> r.getUser().getId().equals(currentUserId)))
                        .reactors(e.getValue().stream().map(r -> r.getUser().getUsername()).toList())
                        .build())
                .toList();

        return DirectMessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .createdAt(message.getCreatedAt())
                .reactions(reactionResponses)
                .build();
    }
}
