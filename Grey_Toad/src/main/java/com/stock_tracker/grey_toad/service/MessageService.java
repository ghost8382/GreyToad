package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.MessageRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.MessageResponse;
import com.stock_tracker.grey_toad.entity.Channel;
import com.stock_tracker.grey_toad.entity.Message;
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

    public MessageService(MessageRepository messageRepository,
                          ChannelRepository channelRepository,
                          UserRepository userRepository,
                          TeamMemberRepository teamMemberRepository) {
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
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

        return mapToResponse(messageRepository.save(message), 0);
    }

    public List<MessageResponse> getByChannel(UUID channelId) {
        List<Message> all = messageRepository.findByChannelId(channelId);

        Map<UUID, Long> replyCounts = all.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(Message::getParentId, Collectors.counting()));

        return all.stream()
                .filter(m -> m.getParentId() == null && m.getType() == MessageType.CHAT)
                .map(m -> mapToResponse(m, replyCounts.getOrDefault(m.getId(), 0L).intValue()))
                .toList();
    }

    public List<MessageResponse> getReplies(UUID parentId) {
        return messageRepository.findByParentIdOrderByCreatedAtAsc(parentId)
                .stream().map(m -> mapToResponse(m, 0)).toList();
    }

    public List<MessageResponse> getThreadStarters(UUID channelId) {
        List<Message> all = messageRepository.findByChannelId(channelId);

        Map<UUID, Long> replyCounts = all.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(Message::getParentId, Collectors.counting()));

        return all.stream()
                .filter(m -> m.getParentId() == null && m.getType() == MessageType.CHAT && replyCounts.containsKey(m.getId()))
                .map(m -> mapToResponse(m, replyCounts.get(m.getId()).intValue()))
                .toList();
    }

    public List<MessageResponse> getPosts(UUID channelId) {
        return messageRepository.findByChannelIdAndTypeOrderByCreatedAtDesc(channelId, MessageType.POST)
                .stream().map(m -> mapToResponse(m, 0)).toList();
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

        return mapToResponse(messageRepository.save(post), 0);
    }

    private User findUserByEmail(String senderEmail) {
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }
        return userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private MessageResponse mapToResponse(Message message, int replyCount) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .channelId(message.getChannel().getId())
                .parentId(message.getParentId())
                .createdAt(message.getCreatedAt())
                .replyCount(replyCount)
                .type(message.getType() != null ? message.getType() : MessageType.CHAT)
                .build();
    }
}
