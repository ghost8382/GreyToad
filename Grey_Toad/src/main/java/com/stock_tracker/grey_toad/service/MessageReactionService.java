package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.MessageReactionRepository;
import com.stock_tracker.grey_toad.data.MessageRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.MessageReactionResponse;
import com.stock_tracker.grey_toad.entity.Message;
import com.stock_tracker.grey_toad.entity.MessageReaction;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageReactionService {

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageReactionService(MessageReactionRepository reactionRepository,
                                  MessageRepository messageRepository,
                                  UserRepository userRepository) {
        this.reactionRepository = reactionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<MessageReactionResponse> toggle(UUID messageId, String userEmail, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var existing = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            MessageReaction reaction = new MessageReaction();
            reaction.setMessage(message);
            reaction.setUser(user);
            reaction.setEmoji(emoji);
            reactionRepository.save(reaction);
        }

        return getForMessage(messageId, user.getId());
    }

    public List<MessageReactionResponse> getForMessage(UUID messageId, UUID currentUserId) {
        List<MessageReaction> all = reactionRepository.findByMessageId(messageId);

        Map<String, List<MessageReaction>> byEmoji = all.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji));

        return byEmoji.entrySet().stream()
                .map(e -> MessageReactionResponse.builder()
                        .emoji(e.getKey())
                        .count(e.getValue().size())
                        .reactedByMe(e.getValue().stream().anyMatch(r -> r.getUser().getId().equals(currentUserId)))
                        .build())
                .toList();
    }
}
