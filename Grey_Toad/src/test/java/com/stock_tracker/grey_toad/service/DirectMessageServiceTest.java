package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.DirectMessageRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.DirectMessageResponse;
import com.stock_tracker.grey_toad.entity.DirectMessage;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DirectMessageService directMessageService;

    @Test
    void sendPersistsDirectMessage() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(senderId, "sender@example.com", "sender");
        User receiver = user(receiverId, "receiver@example.com", "receiver");

        DirectMessage savedMessage = new DirectMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setSender(sender);
        savedMessage.setReceiver(receiver);
        savedMessage.setContent("hello");
        savedMessage.setCreatedAt(LocalDateTime.of(2026, 4, 16, 12, 0));

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(repository.save(any(DirectMessage.class))).thenReturn(savedMessage);

        DirectMessageResponse response = directMessageService.send("hello", senderId, receiverId);

        assertEquals(savedMessage.getId(), response.getId());
        assertEquals(senderId, response.getSenderId());
        assertEquals(receiverId, response.getReceiverId());
        assertEquals("hello", response.getContent());
    }

    @Test
    void sendRejectsSelfMessaging() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "self@example.com", "self");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> directMessageService.send("hello", userId, userId));
    }

    @Test
    void getConversationReturnsOrderedMessagesForBothDirections() {
        UUID requesterId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        User requester = user(requesterId, "requester@example.com", "requester");
        User otherUser = user(otherUserId, "other@example.com", "other");

        DirectMessage first = directMessage(UUID.randomUUID(), requester, otherUser, "first", LocalDateTime.of(2026, 4, 16, 10, 0));
        DirectMessage second = directMessage(UUID.randomUUID(), otherUser, requester, "second", LocalDateTime.of(2026, 4, 16, 10, 5));

        when(userRepository.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(repository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                requesterId,
                otherUserId,
                otherUserId,
                requesterId
        )).thenReturn(List.of(first, second));

        List<DirectMessageResponse> conversation = directMessageService.getConversation("requester@example.com", otherUserId);

        assertEquals(2, conversation.size());
        assertEquals("first", conversation.get(0).getContent());
        assertEquals("second", conversation.get(1).getContent());
        assertEquals(requesterId, conversation.get(0).getSenderId());
        assertEquals(requesterId, conversation.get(1).getReceiverId());
    }

    private User user(UUID id, String email, String username) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(username);
        return user;
    }

    private DirectMessage directMessage(UUID id, User sender, User receiver, String content, LocalDateTime createdAt) {
        DirectMessage message = new DirectMessage();
        message.setId(id);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setCreatedAt(createdAt);
        return message;
    }
}
