package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.ChannelRepository;
import com.stock_tracker.grey_toad.data.TeamMemberRepository;
import com.stock_tracker.grey_toad.data.TeamRepository;
import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.entity.Channel;
import com.stock_tracker.grey_toad.entity.Team;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.dto.ChannelResponse;
import com.stock_tracker.grey_toad.dto.CreateChannelRequest;
import com.stock_tracker.grey_toad.exceptions.ForbiddenException;
import com.stock_tracker.grey_toad.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    public ChannelService(ChannelRepository channelRepository, TeamRepository teamRepository,
                          UserRepository userRepository, TeamMemberRepository teamMemberRepository) {
        this.channelRepository = channelRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    public ChannelResponse create(CreateChannelRequest request) {

        UUID teamId = UUID.fromString(request.getTeamId());

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setTeam(team);

        Channel saved = channelRepository.save(channel);

        return mapToResponse(saved);
    }

    public List<ChannelResponse> getByTeam(UUID teamId, String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            if (!"ADMIN".equals(user.getRole()) &&
                    !teamMemberRepository.existsByUserIdAndTeamId(user.getId(), teamId)) {
                throw new ForbiddenException("Access denied: you are not a member of this team");
            }
        }
        return channelRepository.findByTeamId(teamId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ChannelResponse mapToResponse(Channel channel) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .teamId(channel.getTeam().getId())
                .build();
    }
}