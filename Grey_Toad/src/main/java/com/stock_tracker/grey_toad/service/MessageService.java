package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.MessageRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.MessageResponse;
import com.stock_tracker.grey_toad.entity.Channel;
import com.stock_tracker.grey_toad.entity.Message;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        return mapToResponse(saved);
    }

    private User findUserByEmail(String senderEmail) {
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new UnauthorizedException("Unauthorized user");
        }

        return userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public List<MessageResponse> getByChannel(UUID channelId) {
        return messageRepository.findByChannelId(channelId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSender().getId())
                .channelId(message.getChannel().getId())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
