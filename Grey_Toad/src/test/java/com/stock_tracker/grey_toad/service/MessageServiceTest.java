package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.MessageRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.entity.Channel;
import com.stock_tracker.grey_toad.entity.Team;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendRejectsUserOutsideChannelTeam() {
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail("dev@example.com");

        Team team = new Team();
        team.setId(teamId);

        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setTeam(team);

        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(user));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(teamMemberRepository.existsByUserIdAndTeamId(userId, teamId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> messageService.send(channelId, "dev@example.com", "hello"));
    }
}
