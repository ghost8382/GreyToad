package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.MessageReactionRepository;
import com.stock_tracker.grey_toad.data.MessageRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.MessageReactionResponse;
import com.stock_tracker.grey_toad.dto.MessageResponse;
import com.stock_tracker.grey_toad.entity.Channel;
import com.stock_tracker.grey_toad.entity.Message;
import com.stock_tracker.grey_toad.entity.MessageReaction;
import com.stock_tracker.grey_toad.entity.MessageType;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MessageReactionRepository reactionRepository;

    public MessageService(MessageRepository messageRepository,
                          ChannelRepository channelRepository,
                          UserRepository userRepository,
                          TeamMemberRepository teamMemberRepository,
                          MessageReactionRepository reactionRepository) {
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.reactionRepository = reactionRepository;
    }

    public MessageResponse send(UUID channelId, String senderEmail, String content) {
        return send(channelId, senderEmail, content, null);
    }

    public MessageResponse send(UUID channelId, String senderEmail, String content, UUID parentId) {
        User sender = findUserByEmail(senderEmail);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel not found"));

        if (!teamMemberRepository.existsByUserIdAndTeamId(sender.getId(), channel.getTeam().getId())) {
            throw new ForbiddenException("User is not a member of this team");
        }

        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        message.setChannel(channel);
        message.setParentId(parentId);
        message.setCreatedAt(LocalDateTime.now());

        return mapToResponse(messageRepository.save(message), 0, List.of(), null);
    }

    public List<MessageResponse> getByChannel(UUID channelId, String currentUserEmail) {
        List<Message> all = messageRepository.findByChannelId(channelId);
        Map<UUID, List<MessageReaction>> reactionsMap = batchLoadReactions(all);
        UUID currentUserId = resolveUserId(currentUserEmail);

        Map<UUID, Long> replyCounts = all.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(Message::getParentId, Collectors.counting()));

        return all.stream()
                .filter(m -> m.getParentId() == null && m.getType() == MessageType.CHAT)
                .map(m -> mapToResponse(m, replyCounts.getOrDefault(m.getId(), 0L).intValue(),
                        reactionsMap.getOrDefault(m.getId(), List.of()), currentUserId))
                .toList();
    }

    public List<MessageResponse> getReplies(UUID parentId, String currentUserEmail) {
        List<Message> replies = messageRepository.findByParentIdOrderByCreatedAtAsc(parentId);
        Map<UUID, List<MessageReaction>> reactionsMap = batchLoadReactions(replies);
        UUID currentUserId = resolveUserId(currentUserEmail);
        return replies.stream()
                .map(m -> mapToResponse(m, 0, reactionsMap.getOrDefault(m.getId(), List.of()), currentUserId))
                .toList();
    }

    public List<MessageResponse> getThreadStarters(UUID channelId) {
        List<Message> all = messageRepository.findByChannelId(channelId);
        Map<UUID, Long> replyCounts = all.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(Message::getParentId, Collectors.counting()));
        return all.stream()
                .filter(m -> m.getParentId() == null && m.getType() == MessageType.CHAT && replyCounts.containsKey(m.getId()))
                .map(m -> mapToResponse(m, replyCounts.get(m.getId()).intValue(), List.of(), null))
                .toList();
    }

    public List<MessageResponse> getPosts(UUID channelId, String currentUserEmail) {
        List<Message> posts = messageRepository.findByChannelIdAndTypeOrderByCreatedAtDesc(channelId, MessageType.POST);
        Map<UUID, List<MessageReaction>> reactionsMap = batchLoadReactions(posts);
        UUID currentUserId = resolveUserId(currentUserEmail);
        return posts.stream()
                .map(m -> mapToResponse(m, 0, reactionsMap.getOrDefault(m.getId(), List.of()), currentUserId))
                .toList();
    }

    public MessageResponse resolvePost(UUID messageId, String userEmail) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User user = findUserByEmail(userEmail);
        if (!"ADMIN".equals(user.getRole()) && !message.getSender().getId().equals(user.getId())) {
            throw new ForbiddenException("Only admin or post author can resolve");
        }
        message.setResolved(!message.isResolved());
        Message saved = messageRepository.save(message);
        return mapToResponse(saved, 0, reactionRepository.findByMessageId(saved.getId()), user.getId());
    }

    public MessageResponse createPost(UUID channelId, String senderEmail, String content) {
        User sender = findUserByEmail(senderEmail);
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel not found"));

        Message post = new Message();
        post.setContent(content);
        post.setSender(sender);
        post.setChannel(channel);
        post.setType(MessageType.POST);
        post.setCreatedAt(LocalDateTime.now());

        return mapToResponse(messageRepository.save(post), 0, List.of(), null);
    }

    private User findUserByEmail(String senderEmail) {
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }
        return userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UUID resolveUserId(String email) {
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private Map<UUID, List<MessageReaction>> batchLoadReactions(List<Message> messages) {
        if (messages.isEmpty()) return Map.of();
        List<UUID> ids = messages.stream().map(Message::getId).toList();
        return reactionRepository.findByMessageIdIn(ids).stream()
                .collect(Collectors.groupingBy(r -> r.getMessage().getId()));
    }

    private MessageResponse mapToResponse(Message message, int replyCount,
                                           List<MessageReaction> reactions, UUID currentUserId) {
        Map<String, List<MessageReaction>> byEmoji = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji));

        List<MessageReactionResponse> reactionResponses = byEmoji.entrySet().stream()
                .map(e -> MessageReactionResponse.builder()
                        .emoji(e.getKey())
                        .count(e.getValue().size())
                        .reactedByMe(currentUserId != null &&
                                e.getValue().stream().anyMatch(r -> r.getUser().getId().equals(currentUserId)))
                        .build())
                .toList();

        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .channelId(message.getChannel().getId())
                .parentId(message.getParentId())
                .createdAt(message.getCreatedAt())
                .replyCount(replyCount)
                .reactions(reactionResponses)
                .type(message.getType() != null ? message.getType() : MessageType.CHAT)
                .resolved(message.isResolved())
                .build();
    }
}
