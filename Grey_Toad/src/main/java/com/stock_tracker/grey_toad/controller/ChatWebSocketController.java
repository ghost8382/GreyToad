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
import com.stock_tracker.grey_toad.service.AppNotificationService;
import com.stock_tracker.grey_toad.service.DirectMessageService;
import com.stock_tracker.grey_toad.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final DirectMessageService directMessageService;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AppNotificationService appNotificationService;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                   MessageService messageService,
                                   DirectMessageService directMessageService,
                                   UserRepository userRepository,
                                   ChannelRepository channelRepository,
                                   TeamMemberRepository teamMemberRepository,
                                   AppNotificationService appNotificationService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.directMessageService = directMessageService;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.appNotificationService = appNotificationService;
    }

    @MessageMapping("/chat.send/{channelId}")
    public void sendMessage(@DestinationVariable UUID channelId,
                            CreateMessageRequest request,
                            Principal principal) {
        User sender = requireUser(principal);
        MessageResponse saved = messageService.send(channelId, sender.getEmail(), request.getContent());

        messagingTemplate.convertAndSend("/topic/channel/" + channelId, saved);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel not found"));

        AppNotificationDto channelNotif = AppNotificationDto.builder()
                .type("CHANNEL_MESSAGE")
                .title("New message in #" + channel.getName())
                .body(truncate(request.getContent(), 80))
                .build();

        List<User> allUsers = userRepository.findAll();
        Set<UUID> mentionedIds = findMentionedUserIds(request.getContent(), allUsers, sender.getId());

        for (User u : allUsers) {
            if (mentionedIds.contains(u.getId())) {
                AppNotificationDto mentionNotif = AppNotificationDto.builder()
                        .type("MENTION")
                        .title(sender.getUsername() + " mentioned you in #" + channel.getName())
                        .body(truncate(request.getContent(), 80))
                        .build();
                messagingTemplate.convertAndSendToUser(u.getEmail(), "/queue/notifications", mentionNotif);
                // Only persist for offline users
                if (!u.isOnline()) {
                    appNotificationService.persist(
                            u.getId(), "MENTION",
                            mentionNotif.getTitle(), mentionNotif.getBody(),
                            channel.getTeam() != null && channel.getTeam().getProject() != null
                                    ? channel.getTeam().getProject().getId().toString() : null
                    );
                }
            }
        }

        teamMemberRepository.findByTeamId(channel.getTeam().getId()).stream()
                .map(TeamMember::getUser)
                .filter(u -> !u.getId().equals(sender.getId()))
                .filter(u -> !mentionedIds.contains(u.getId()))
                .forEach(u -> messagingTemplate.convertAndSendToUser(
                        u.getEmail(), "/queue/notifications", channelNotif));
    }

    private Set<UUID> findMentionedUserIds(String content, List<User> allUsers, UUID senderId) {
        Set<UUID> result = new HashSet<>();
        if (content == null || content.isEmpty()) return result;
        for (User u : allUsers) {
            if (u.getId().equals(senderId)) continue;
            String tag = "@" + u.getUsername();
            int idx = content.indexOf(tag);
            if (idx < 0) continue;
            int afterIdx = idx + tag.length();
            if (afterIdx >= content.length() || !Character.isLetterOrDigit(content.charAt(afterIdx))) {
                result.add(u.getId());
            }
        }
        return result;
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

        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/dm", saved);

        AppNotificationDto notification = AppNotificationDto.builder()
                .type("DM")
                .title("New message from " + sender.getUsername())
                .body(truncate(request.getContent(), 80))
                .build();

        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/notifications", notification);

        // Only persist for offline receivers — online users receive via WS
        if (!receiver.isOnline()) {
            appNotificationService.persist(
                    receiver.getId(), "DM",
                    notification.getTitle(), notification.getBody(), null
            );
        }
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
