package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.AppNotificationDto;
import com.stock_tracker.grey_toad.dto.CreateDirectMessageRequest;
import com.stock_tracker.grey_toad.dto.CreateMessageRequest;
import com.stock_tracker.grey_toad.dto.DirectMessageResponse;
import com.stock_tracker.grey_toad.dto.MessageResponse;
import com.stock_tracker.grey_toad.entity.Channel;
import com.stock_tracker.grey_toad.entity.TeamMember;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.BadRequestException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.service.DirectMessageService;
import com.stock_tracker.grey_toad.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final DirectMessageService directMessageService;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final TeamMemberRepository teamMemberRepository;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                   MessageService messageService,
                                   DirectMessageService directMessageService,
                                   UserRepository userRepository,
                                   ChannelRepository channelRepository,
                                   TeamMemberRepository teamMemberRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.directMessageService = directMessageService;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @MessageMapping("/chat.send/{channelId}")
    public void sendMessage(@DestinationVariable UUID channelId,
                            CreateMessageRequest request,
                            Principal principal) {
        User sender = requireUser(principal);
        MessageResponse saved = messageService.send(channelId, sender.getEmail(), request.getContent());

        // Broadcast message to everyone subscribed to this channel topic
        messagingTemplate.convertAndSend("/topic/channel/" + channelId, saved);

        // Send notification to all team members except the sender
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel not found"));

        AppNotificationDto notification = AppNotificationDto.builder()
                .type("CHANNEL_MESSAGE")
                .title("Nowa wiadomość w #" + channel.getName())
                .body(truncate(request.getContent(), 80))
                .build();

        teamMemberRepository.findByTeamId(channel.getTeam().getId()).stream()
                .map(TeamMember::getUser)
                .filter(u -> !u.getId().equals(sender.getId()))
                .forEach(u -> messagingTemplate.convertAndSendToUser(
                        u.getEmail(), "/queue/notifications", notification));
    }

    @MessageMapping("/dm.send")
    public void sendDirectMessage(CreateDirectMessageRequest request,
                                  Principal principal) {
        User sender = requireUser(principal);
        UUID receiverId = parseUuid(request.getReceiverId(), "receiverId");

        DirectMessageResponse saved = directMessageService.send(
                request.getContent(),
                sender.getId(),
                receiverId
        );

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Deliver the message payload to the receiver's chat view
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/dm", saved);

        // Send notification to receiver via the always-active notifications queue
        AppNotificationDto notification = AppNotificationDto.builder()
                .type("DM")
                .title("Nowa wiadomość od " + sender.getUsername())
                .body(truncate(request.getContent(), 80))
                .build();

        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/notifications", notification);
    }

    private User requireUser(Principal principal) {
        if (principal == null) throw new UnauthorizedException("Unauthorized websocket user");
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid " + fieldName);
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
