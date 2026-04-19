package com.stock_tracker.grey_toad.controller;

import com.stock_tracker.grey_toad.dto.DirectMessageResponse;
import com.stock_tracker.grey_toad.service.DirectMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DirectMessageControllerTest {

    @Mock
    private DirectMessageService directMessageService;

    @InjectMocks
    private DirectMessageController directMessageController;

    @Test
    void getConversationReturnsHistoryForAuthenticatedUser() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(directMessageController).build();

        UUID otherUserId = UUID.randomUUID();
        DirectMessageResponse first = DirectMessageResponse.builder()
                .id(UUID.randomUUID())
                .content("first")
                .senderId(UUID.randomUUID())
                .receiverId(otherUserId)
                .createdAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                .build();

        when(directMessageService.getConversation("user@example.com", otherUserId)).thenReturn(List.of(first));

        mockMvc.perform(get("/direct-messages/{otherUserId}", otherUserId)
                        .principal(principal("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("first"))
                .andExpect(jsonPath("$[0].receiverId").value(otherUserId.toString()));

        verify(directMessageService).getConversation("user@example.com", otherUserId);
    }

    private Principal principal(String name) {
        return () -> name;
    }
}
