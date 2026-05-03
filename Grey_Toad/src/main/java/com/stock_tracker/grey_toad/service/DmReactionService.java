package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.DirectMessageRepository;
import com.stock_tracker.grey_toad.data.DmReactionRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.MessageReactionResponse;
import com.stock_tracker.grey_toad.entity.DirectMessage;
import com.stock_tracker.grey_toad.entity.DmReaction;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DmReactionService {

    private final DmReactionRepository reactionRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    public DmReactionService(DmReactionRepository reactionRepository,
                             DirectMessageRepository directMessageRepository,
                             UserRepository userRepository) {
        this.reactionRepository = reactionRepository;
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<MessageReactionResponse> toggle(UUID dmId, String userEmail, String emoji) {
        DirectMessage dm = directMessageRepository.findById(dmId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var existing = reactionRepository.findByDirectMessageIdAndUserIdAndEmoji(dmId, user.getId(), emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            DmReaction reaction = new DmReaction();
            reaction.setDirectMessage(dm);
            reaction.setUser(user);
            reaction.setEmoji(emoji);
            reactionRepository.save(reaction);
        }

        return buildResponse(dmId, user.getId());
    }

    private List<MessageReactionResponse> buildResponse(UUID dmId, UUID currentUserId) {
        List<DmReaction> all = reactionRepository.findByDirectMessageId(dmId);
        Map<String, List<DmReaction>> byEmoji = all.stream()
                .collect(Collectors.groupingBy(DmReaction::getEmoji));
        return byEmoji.entrySet().stream()
                .map(e -> MessageReactionResponse.builder()
                        .emoji(e.getKey())
                        .count(e.getValue().size())
                        .reactedByMe(e.getValue().stream().anyMatch(r -> r.getUser().getId().equals(currentUserId)))
                        .build())
                .toList();
    }
}
